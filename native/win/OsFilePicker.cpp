#include <jni.h>
#include <windows.h>
#include <shobjidl.h>
#include <shlobj.h>
#include <oleidl.h>
#include <string>
#include <vector>

namespace {

std::wstring jstring_to_wstring(JNIEnv* env, jstring s) {
    if (!s) return L"";
    const jchar* chars = env->GetStringChars(s, nullptr);
    jsize len = env->GetStringLength(s);
    std::wstring out(reinterpret_cast<const wchar_t*>(chars), static_cast<size_t>(len));
    env->ReleaseStringChars(s, chars);
    return out;
}

jstring wstring_to_jstring(JNIEnv* env, const std::wstring& s) {
    return env->NewString(reinterpret_cast<const jchar*>(s.c_str()), static_cast<jsize>(s.size()));
}

struct ComInit {
    HRESULT hr;
    ComInit() { hr = CoInitializeEx(nullptr, COINIT_APARTMENTTHREADED | COINIT_DISABLE_OLE1DDE); }
    ~ComInit() { if (SUCCEEDED(hr)) CoUninitialize(); }
};

void apply_filters(IFileDialog* dlg, JNIEnv* env, jobjectArray names, jobjectArray specs,
                   std::vector<std::wstring>& storage) {
    if (!names || !specs) return;
    jsize count = env->GetArrayLength(names);
    if (count == 0) return;

    storage.reserve(static_cast<size_t>(count) * 2);
    std::vector<COMDLG_FILTERSPEC> filters;
    filters.reserve(count);

    for (jsize i = 0; i < count; i++) {
        jstring jn = static_cast<jstring>(env->GetObjectArrayElement(names, i));
        jstring js = static_cast<jstring>(env->GetObjectArrayElement(specs, i));
        storage.push_back(jstring_to_wstring(env, jn));
        storage.push_back(jstring_to_wstring(env, js));
        if (jn) env->DeleteLocalRef(jn);
        if (js) env->DeleteLocalRef(js);
    }

    for (jsize i = 0; i < count; i++) {
        COMDLG_FILTERSPEC f;
        f.pszName = storage[static_cast<size_t>(i) * 2].c_str();
        f.pszSpec = storage[static_cast<size_t>(i) * 2 + 1].c_str();
        filters.push_back(f);
    }

    dlg->SetFileTypes(static_cast<UINT>(filters.size()), filters.data());
    dlg->SetFileTypeIndex(1);
}

void apply_initial_dir(IFileDialog* dlg, JNIEnv* env, jstring initialDir) {
    if (!initialDir) return;
    std::wstring dir = jstring_to_wstring(env, initialDir);
    if (dir.empty()) return;
    IShellItem* psi = nullptr;
    if (SUCCEEDED(SHCreateItemFromParsingName(dir.c_str(), nullptr, IID_PPV_ARGS(&psi)))) {
        dlg->SetFolder(psi);
        psi->Release();
    }
}

void apply_title(IFileDialog* dlg, JNIEnv* env, jstring title) {
    if (!title) return;
    std::wstring t = jstring_to_wstring(env, title);
    if (!t.empty()) dlg->SetTitle(t.c_str());
}

jstring shell_item_to_jstring(JNIEnv* env, IShellItem* item) {
    if (!item) return nullptr;
    PWSTR path = nullptr;
    if (FAILED(item->GetDisplayName(SIGDN_FILESYSPATH, &path))) return nullptr;
    jstring out = wstring_to_jstring(env, path);
    CoTaskMemFree(path);
    return out;
}

class FileDialogEvents : public IFileDialogEvents {
public:
    LONG refCount = 1;
    bool selectFolderRequested = false;
    bool initialized = false;
    bool openClicked = false;
    bool subclassed = false;
    IFileDialog* dialogRef = nullptr;
    IShellItem* capturedFolder = nullptr;
    HWND okButton = nullptr;
    WNDPROC originalOkProc = nullptr;

    static FileDialogEvents* current;

    ~FileDialogEvents() {
        unsubclass();
        if (capturedFolder) capturedFolder->Release();
    }

    HRESULT STDMETHODCALLTYPE QueryInterface(REFIID riid, void** ppv) override {
        if (!ppv) return E_POINTER;
        if (riid == IID_IUnknown || riid == IID_IFileDialogEvents) {
            *ppv = static_cast<IFileDialogEvents*>(this);
        } else {
            *ppv = nullptr;
            return E_NOINTERFACE;
        }
        AddRef();
        return S_OK;
    }
    ULONG STDMETHODCALLTYPE AddRef() override { return InterlockedIncrement(&refCount); }
    ULONG STDMETHODCALLTYPE Release() override {
        LONG c = InterlockedDecrement(&refCount);
        if (c == 0) delete this;
        return c;
    }

    static LRESULT CALLBACK OkButtonProc(HWND hwnd, UINT msg, WPARAM wp, LPARAM lp) {
        FileDialogEvents* self = current;
        if (self) {
            if (msg == WM_LBUTTONUP || msg == BM_CLICK) {
                self->openClicked = true;
            }
        }
        WNDPROC original = self ? self->originalOkProc : nullptr;
        return original
            ? CallWindowProc(original, hwnd, msg, wp, lp)
            : DefWindowProc(hwnd, msg, wp, lp);
    }

    void setupSubclass() {
        if (subclassed || !dialogRef) return;
        IOleWindow* ole = nullptr;
        if (FAILED(dialogRef->QueryInterface(IID_PPV_ARGS(&ole))) || !ole) return;
        HWND dlgHwnd = nullptr;
        ole->GetWindow(&dlgHwnd);
        ole->Release();
        if (!dlgHwnd) return;

        okButton = GetDlgItem(dlgHwnd, IDOK);
        if (!okButton) {
            EnumChildWindows(dlgHwnd, [](HWND child, LPARAM lp) -> BOOL {
                if (GetDlgCtrlID(child) == IDOK) {
                    *reinterpret_cast<HWND*>(lp) = child;
                    return FALSE;
                }
                return TRUE;
            }, reinterpret_cast<LPARAM>(&okButton));
        }
        if (!okButton) return;

        current = this;
        originalOkProc = reinterpret_cast<WNDPROC>(SetWindowLongPtr(
            okButton, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(OkButtonProc)));
        subclassed = true;
    }

    void unsubclass() {
        if (!subclassed) return;
        if (okButton && originalOkProc) {
            SetWindowLongPtr(okButton, GWLP_WNDPROC, reinterpret_cast<LONG_PTR>(originalOkProc));
        }
        if (current == this) current = nullptr;
        subclassed = false;
        okButton = nullptr;
        originalOkProc = nullptr;
    }

    HRESULT STDMETHODCALLTYPE OnFileOk(IFileDialog*) override { return S_OK; }

    HRESULT STDMETHODCALLTYPE OnFolderChanging(IFileDialog*, IShellItem* psiFolder) override {
        if (!initialized || !openClicked) return S_OK;
        openClicked = false;
        if (psiFolder && dialogRef) {
            if (capturedFolder) capturedFolder->Release();
            capturedFolder = psiFolder;
            capturedFolder->AddRef();
            selectFolderRequested = true;
            dialogRef->Close(S_OK);
            return S_FALSE;
        }
        return S_OK;
    }

    HRESULT STDMETHODCALLTYPE OnFolderChange(IFileDialog*) override {
        initialized = true;
        if (!subclassed) setupSubclass();
        return S_OK;
    }
    HRESULT STDMETHODCALLTYPE OnSelectionChange(IFileDialog*) override { return S_OK; }
    HRESULT STDMETHODCALLTYPE OnShareViolation(IFileDialog*, IShellItem*, FDE_SHAREVIOLATION_RESPONSE*) override { return S_OK; }
    HRESULT STDMETHODCALLTYPE OnTypeChange(IFileDialog*) override { return S_OK; }
    HRESULT STDMETHODCALLTYPE OnOverwrite(IFileDialog*, IShellItem*, FDE_OVERWRITE_RESPONSE*) override { return S_OK; }
};

FileDialogEvents* FileDialogEvents::current = nullptr;

}

extern "C" {

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFile(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    ComInit com;

    IFileOpenDialog* dlg = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dlg));
    if (FAILED(hr) || !dlg) return nullptr;

    apply_title(dlg, env, title);
    std::vector<std::wstring> storage;
    apply_filters(dlg, env, filterNames, filterSpecs, storage);
    apply_initial_dir(dlg, env, initialDir);

    jstring result = nullptr;
    if (SUCCEEDED(dlg->Show(nullptr))) {
        IShellItem* item = nullptr;
        if (SUCCEEDED(dlg->GetResult(&item))) {
            result = shell_item_to_jstring(env, item);
            if (item) item->Release();
        }
    }
    dlg->Release();
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFiles(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    ComInit com;

    IFileOpenDialog* dlg = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dlg));
    if (FAILED(hr) || !dlg) return nullptr;

    DWORD opts = 0;
    dlg->GetOptions(&opts);
    dlg->SetOptions(opts | FOS_ALLOWMULTISELECT);

    apply_title(dlg, env, title);
    std::vector<std::wstring> storage;
    apply_filters(dlg, env, filterNames, filterSpecs, storage);
    apply_initial_dir(dlg, env, initialDir);

    jobjectArray result = nullptr;
    if (SUCCEEDED(dlg->Show(nullptr))) {
        IShellItemArray* items = nullptr;
        if (SUCCEEDED(dlg->GetResults(&items)) && items) {
            DWORD count = 0;
            items->GetCount(&count);
            jclass strClass = env->FindClass("java/lang/String");
            result = env->NewObjectArray(static_cast<jsize>(count), strClass, nullptr);
            for (DWORD i = 0; i < count; i++) {
                IShellItem* item = nullptr;
                if (SUCCEEDED(items->GetItemAt(i, &item))) {
                    jstring s = shell_item_to_jstring(env, item);
                    if (s) {
                        env->SetObjectArrayElement(result, static_cast<jsize>(i), s);
                        env->DeleteLocalRef(s);
                    }
                    item->Release();
                }
            }
            items->Release();
        }
    }
    dlg->Release();
    return result;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nSaveFile(
        JNIEnv* env, jclass, jstring title, jstring initialDir, jstring defaultName,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    ComInit com;

    IFileSaveDialog* dlg = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_FileSaveDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dlg));
    if (FAILED(hr) || !dlg) return nullptr;

    apply_title(dlg, env, title);
    std::vector<std::wstring> storage;
    apply_filters(dlg, env, filterNames, filterSpecs, storage);
    apply_initial_dir(dlg, env, initialDir);

    if (defaultName) {
        std::wstring nm = jstring_to_wstring(env, defaultName);
        if (!nm.empty()) dlg->SetFileName(nm.c_str());
    }

    jstring result = nullptr;
    if (SUCCEEDED(dlg->Show(nullptr))) {
        IShellItem* item = nullptr;
        if (SUCCEEDED(dlg->GetResult(&item))) {
            result = shell_item_to_jstring(env, item);
            if (item) item->Release();
        }
    }
    dlg->Release();
    return result;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFileOrDirectory(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    ComInit com;

    IFileOpenDialog* dlg = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dlg));
    if (FAILED(hr) || !dlg) return nullptr;

    apply_title(dlg, env, title);
    std::vector<std::wstring> storage;
    apply_filters(dlg, env, filterNames, filterSpecs, storage);
    apply_initial_dir(dlg, env, initialDir);

    FileDialogEvents* events = new FileDialogEvents();
    events->dialogRef = dlg;
    DWORD cookie = 0;
    dlg->Advise(events, &cookie);

    jstring result = nullptr;
    HRESULT showHr = dlg->Show(nullptr);
    if (events->selectFolderRequested && events->capturedFolder) {
        result = shell_item_to_jstring(env, events->capturedFolder);
    } else if (SUCCEEDED(showHr)) {
        IShellItem* item = nullptr;
        if (SUCCEEDED(dlg->GetResult(&item))) {
            result = shell_item_to_jstring(env, item);
            if (item) item->Release();
        }
    }

    dlg->Unadvise(cookie);
    events->Release();
    dlg->Release();
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFilesOrDirectories(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    ComInit com;

    IFileOpenDialog* dlg = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dlg));
    if (FAILED(hr) || !dlg) return nullptr;

    DWORD opts = 0;
    dlg->GetOptions(&opts);
    dlg->SetOptions(opts | FOS_ALLOWMULTISELECT);

    apply_title(dlg, env, title);
    std::vector<std::wstring> storage;
    apply_filters(dlg, env, filterNames, filterSpecs, storage);
    apply_initial_dir(dlg, env, initialDir);

    FileDialogEvents* events = new FileDialogEvents();
    events->dialogRef = dlg;
    DWORD cookie = 0;
    dlg->Advise(events, &cookie);

    jobjectArray result = nullptr;
    HRESULT showHr = dlg->Show(nullptr);
    jclass strClass = env->FindClass("java/lang/String");

    if (events->selectFolderRequested && events->capturedFolder) {
        jstring s = shell_item_to_jstring(env, events->capturedFolder);
        if (s) {
            result = env->NewObjectArray(1, strClass, nullptr);
            env->SetObjectArrayElement(result, 0, s);
            env->DeleteLocalRef(s);
        }
    } else if (SUCCEEDED(showHr)) {
        IShellItemArray* items = nullptr;
        if (SUCCEEDED(dlg->GetResults(&items)) && items) {
            DWORD count = 0;
            items->GetCount(&count);
            result = env->NewObjectArray(static_cast<jsize>(count), strClass, nullptr);
            for (DWORD i = 0; i < count; i++) {
                IShellItem* item = nullptr;
                if (SUCCEEDED(items->GetItemAt(i, &item))) {
                    jstring s = shell_item_to_jstring(env, item);
                    if (s) {
                        env->SetObjectArrayElement(result, static_cast<jsize>(i), s);
                        env->DeleteLocalRef(s);
                    }
                    item->Release();
                }
            }
            items->Release();
        }
    }

    dlg->Unadvise(cookie);
    events->Release();
    dlg->Release();
    return result;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenDirectory(
        JNIEnv* env, jclass, jstring title, jstring initialDir) {
    ComInit com;

    IFileOpenDialog* dlg = nullptr;
    HRESULT hr = CoCreateInstance(CLSID_FileOpenDialog, nullptr, CLSCTX_INPROC_SERVER, IID_PPV_ARGS(&dlg));
    if (FAILED(hr) || !dlg) return nullptr;

    DWORD opts = 0;
    dlg->GetOptions(&opts);
    dlg->SetOptions(opts | FOS_PICKFOLDERS);

    apply_title(dlg, env, title);
    apply_initial_dir(dlg, env, initialDir);

    jstring result = nullptr;
    if (SUCCEEDED(dlg->Show(nullptr))) {
        IShellItem* item = nullptr;
        if (SUCCEEDED(dlg->GetResult(&item))) {
            result = shell_item_to_jstring(env, item);
            if (item) item->Release();
        }
    }
    dlg->Release();
    return result;
}

}
