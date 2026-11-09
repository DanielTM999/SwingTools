#include <jni.h>
#include <gtk/gtk.h>
#include <string>
#include <vector>
#include <sstream>
#include <mutex>
#include <utility>

namespace {

std::mutex g_gtk_mutex;

void ensure_gtk() {
    static bool inited = false;
    if (!inited) {
        int argc = 0;
        char** argv = nullptr;
        gtk_init_check(&argc, &argv);
        inited = true;
    }
}

std::string jstring_to_utf8(JNIEnv* env, jstring s) {
    if (!s) return "";
    const char* chars = env->GetStringUTFChars(s, nullptr);
    std::string out(chars);
    env->ReleaseStringUTFChars(s, chars);
    return out;
}

jstring utf8_to_jstring(JNIEnv* env, const char* s) {
    return s ? env->NewStringUTF(s) : nullptr;
}

std::vector<std::string> split(const std::string& src, char delim) {
    std::vector<std::string> out;
    std::stringstream ss(src);
    std::string tok;
    while (std::getline(ss, tok, delim)) {
        if (!tok.empty()) out.push_back(tok);
    }
    return out;
}

void apply_filters(GtkFileChooser* chooser, JNIEnv* env, jobjectArray names, jobjectArray specs) {
    if (!names || !specs) return;
    jsize count = env->GetArrayLength(names);
    if (count == 0) return;

    for (jsize i = 0; i < count; i++) {
        jstring jn = static_cast<jstring>(env->GetObjectArrayElement(names, i));
        jstring js = static_cast<jstring>(env->GetObjectArrayElement(specs, i));
        std::string name = jstring_to_utf8(env, jn);
        std::string spec = jstring_to_utf8(env, js);

        GtkFileFilter* f = gtk_file_filter_new();
        gtk_file_filter_set_name(f, name.c_str());
        for (const auto& pat : split(spec, ';')) {
            gtk_file_filter_add_pattern(f, pat.c_str());
        }
        gtk_file_chooser_add_filter(chooser, f);

        if (jn) env->DeleteLocalRef(jn);
        if (js) env->DeleteLocalRef(js);
    }
}

void apply_initial_dir(GtkFileChooser* chooser, JNIEnv* env, jstring initialDir) {
    if (!initialDir) return;
    std::string dir = jstring_to_utf8(env, initialDir);
    if (!dir.empty()) gtk_file_chooser_set_current_folder(chooser, dir.c_str());
}

void pump_events_until_destroyed(GtkWidget* dlg) {
    while (gtk_events_pending()) gtk_main_iteration();
    (void)dlg;
}

GtkWidget* make_dialog(const std::string& title, GtkFileChooserAction action,
                       const char* okLabel) {
    GtkWidget* dlg = gtk_file_chooser_dialog_new(
            title.c_str(), nullptr, action,
            "_Cancel", GTK_RESPONSE_CANCEL,
            okLabel, GTK_RESPONSE_ACCEPT,
            nullptr);
    return dlg;
}

}

extern "C" {

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFile(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    std::lock_guard<std::mutex> lock(g_gtk_mutex);
    ensure_gtk();

    std::string t = jstring_to_utf8(env, title);
    if (t.empty()) t = "Open File";

    GtkWidget* dlg = make_dialog(t, GTK_FILE_CHOOSER_ACTION_OPEN, "_Open");
    GtkFileChooser* chooser = GTK_FILE_CHOOSER(dlg);

    apply_filters(chooser, env, filterNames, filterSpecs);
    apply_initial_dir(chooser, env, initialDir);

    jstring result = nullptr;
    if (gtk_dialog_run(GTK_DIALOG(dlg)) == GTK_RESPONSE_ACCEPT) {
        char* fname = gtk_file_chooser_get_filename(chooser);
        if (fname) {
            result = utf8_to_jstring(env, fname);
            g_free(fname);
        }
    }
    gtk_widget_destroy(dlg);
    pump_events_until_destroyed(dlg);
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFiles(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    std::lock_guard<std::mutex> lock(g_gtk_mutex);
    ensure_gtk();

    std::string t = jstring_to_utf8(env, title);
    if (t.empty()) t = "Open Files";

    GtkWidget* dlg = make_dialog(t, GTK_FILE_CHOOSER_ACTION_OPEN, "_Open");
    GtkFileChooser* chooser = GTK_FILE_CHOOSER(dlg);
    gtk_file_chooser_set_select_multiple(chooser, TRUE);

    apply_filters(chooser, env, filterNames, filterSpecs);
    apply_initial_dir(chooser, env, initialDir);

    jobjectArray result = nullptr;
    if (gtk_dialog_run(GTK_DIALOG(dlg)) == GTK_RESPONSE_ACCEPT) {
        GSList* list = gtk_file_chooser_get_filenames(chooser);
        guint len = g_slist_length(list);
        jclass strClass = env->FindClass("java/lang/String");
        result = env->NewObjectArray(static_cast<jsize>(len), strClass, nullptr);
        guint idx = 0;
        for (GSList* it = list; it != nullptr; it = it->next, ++idx) {
            char* path = static_cast<char*>(it->data);
            jstring js = utf8_to_jstring(env, path);
            if (js) {
                env->SetObjectArrayElement(result, static_cast<jsize>(idx), js);
                env->DeleteLocalRef(js);
            }
            g_free(path);
        }
        g_slist_free(list);
    }
    gtk_widget_destroy(dlg);
    pump_events_until_destroyed(dlg);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nSaveFile(
        JNIEnv* env, jclass, jstring title, jstring initialDir, jstring defaultName,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    std::lock_guard<std::mutex> lock(g_gtk_mutex);
    ensure_gtk();

    std::string t = jstring_to_utf8(env, title);
    if (t.empty()) t = "Save File";

    GtkWidget* dlg = make_dialog(t, GTK_FILE_CHOOSER_ACTION_SAVE, "_Save");
    GtkFileChooser* chooser = GTK_FILE_CHOOSER(dlg);
    gtk_file_chooser_set_do_overwrite_confirmation(chooser, TRUE);

    apply_filters(chooser, env, filterNames, filterSpecs);
    apply_initial_dir(chooser, env, initialDir);

    if (defaultName) {
        std::string nm = jstring_to_utf8(env, defaultName);
        if (!nm.empty()) gtk_file_chooser_set_current_name(chooser, nm.c_str());
    }

    jstring result = nullptr;
    if (gtk_dialog_run(GTK_DIALOG(dlg)) == GTK_RESPONSE_ACCEPT) {
        char* fname = gtk_file_chooser_get_filename(chooser);
        if (fname) {
            result = utf8_to_jstring(env, fname);
            g_free(fname);
        }
    }
    gtk_widget_destroy(dlg);
    pump_events_until_destroyed(dlg);
    return result;
}

struct FolderCapture {
    GtkWidget* dlg = nullptr;
    GtkFileChooser* chooser = nullptr;
    bool folderRequested = false;
    std::vector<std::string> capturedPaths;
};

static bool capture_selected_folders(FolderCapture* cap) {
    GSList* selected = gtk_file_chooser_get_filenames(cap->chooser);
    std::vector<std::string> paths;
    bool containsDirectory = false;

    for (GSList* item = selected; item; item = item->next) {
        const char* path = static_cast<const char*>(item->data);
        if (path) {
            paths.emplace_back(path);
            if (g_file_test(path, G_FILE_TEST_IS_DIR))
                containsDirectory = true;
        }
        g_free(item->data);
    }
    g_slist_free(selected);

    if (!containsDirectory) return false;

    cap->capturedPaths = std::move(paths);
    cap->folderRequested = true;
    // Finaliza ANTES de a resposta padrao do GTK navegar na pasta.
    gtk_dialog_response(GTK_DIALOG(cap->dlg), GTK_RESPONSE_ACCEPT);
    return true;
}

static gboolean on_open_button_press(GtkWidget*, GdkEventButton* event, gpointer data) {
    if (event->type != GDK_BUTTON_PRESS || event->button != GDK_BUTTON_PRIMARY)
        return FALSE;
    return capture_selected_folders(static_cast<FolderCapture*>(data)) ? TRUE : FALSE;
}

static gboolean on_open_button_key_press(GtkWidget*, GdkEventKey* event, gpointer data) {
    if (event->keyval != GDK_KEY_Return && event->keyval != GDK_KEY_KP_Enter &&
        event->keyval != GDK_KEY_space)
        return FALSE;
    return capture_selected_folders(static_cast<FolderCapture*>(data)) ? TRUE : FALSE;
}

static void hook_open_button(GtkWidget* dlg, FolderCapture* cap) {
    GtkWidget* openBtn = gtk_dialog_get_widget_for_response(
        GTK_DIALOG(dlg), GTK_RESPONSE_ACCEPT);
    if (!openBtn) return;

    g_signal_connect(openBtn, "button-press-event",
        G_CALLBACK(on_open_button_press), cap);
    g_signal_connect(openBtn, "key-press-event",
        G_CALLBACK(on_open_button_key_press), cap);
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFileOrDirectory(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    std::lock_guard<std::mutex> lock(g_gtk_mutex);
    ensure_gtk();

    std::string t = jstring_to_utf8(env, title);
    if (t.empty()) t = "Open File or Folder";

    GtkWidget* dlg = make_dialog(t, GTK_FILE_CHOOSER_ACTION_OPEN, "_Open");
    GtkFileChooser* chooser = GTK_FILE_CHOOSER(dlg);

    apply_filters(chooser, env, filterNames, filterSpecs);
    apply_initial_dir(chooser, env, initialDir);

    FolderCapture cap;
    cap.dlg = dlg;
    cap.chooser = chooser;
    hook_open_button(dlg, &cap);

    jstring result = nullptr;
    if (gtk_dialog_run(GTK_DIALOG(dlg)) == GTK_RESPONSE_ACCEPT) {
        if (cap.folderRequested) {
            if (!cap.capturedPaths.empty())
                result = utf8_to_jstring(env, cap.capturedPaths.front().c_str());
        } else {
            char* fname = gtk_file_chooser_get_filename(chooser);
            if (fname) {
                result = utf8_to_jstring(env, fname);
                g_free(fname);
            }
        }
    }
    gtk_widget_destroy(dlg);
    pump_events_until_destroyed(dlg);
    return result;
}

JNIEXPORT jobjectArray JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFilesOrDirectories(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    std::lock_guard<std::mutex> lock(g_gtk_mutex);
    ensure_gtk();

    std::string t = jstring_to_utf8(env, title);
    if (t.empty()) t = "Open Files or Folders";

    GtkWidget* dlg = make_dialog(t, GTK_FILE_CHOOSER_ACTION_OPEN, "_Open");
    GtkFileChooser* chooser = GTK_FILE_CHOOSER(dlg);
    gtk_file_chooser_set_select_multiple(chooser, TRUE);

    apply_filters(chooser, env, filterNames, filterSpecs);
    apply_initial_dir(chooser, env, initialDir);

    FolderCapture cap;
    cap.dlg = dlg;
    cap.chooser = chooser;
    hook_open_button(dlg, &cap);

    jobjectArray result = nullptr;
    jclass strClass = env->FindClass("java/lang/String");
    if (gtk_dialog_run(GTK_DIALOG(dlg)) == GTK_RESPONSE_ACCEPT) {
        if (cap.folderRequested) {
            result = env->NewObjectArray(static_cast<jsize>(cap.capturedPaths.size()), strClass, nullptr);
            for (jsize i = 0; i < static_cast<jsize>(cap.capturedPaths.size()); ++i) {
                jstring js = utf8_to_jstring(env, cap.capturedPaths[i].c_str());
                if (js) {
                    env->SetObjectArrayElement(result, i, js);
                    env->DeleteLocalRef(js);
                }
            }
        } else {
            GSList* list = gtk_file_chooser_get_filenames(chooser);
            guint len = g_slist_length(list);
            result = env->NewObjectArray(static_cast<jsize>(len), strClass, nullptr);
            guint idx = 0;
            for (GSList* it = list; it != nullptr; it = it->next, ++idx) {
                char* path = static_cast<char*>(it->data);
                jstring js = utf8_to_jstring(env, path);
                if (js) {
                    env->SetObjectArrayElement(result, static_cast<jsize>(idx), js);
                    env->DeleteLocalRef(js);
                }
                g_free(path);
            }
            g_slist_free(list);
        }
    }
    gtk_widget_destroy(dlg);
    pump_events_until_destroyed(dlg);
    return result;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenDirectory(
        JNIEnv* env, jclass, jstring title, jstring initialDir) {
    std::lock_guard<std::mutex> lock(g_gtk_mutex);
    ensure_gtk();

    std::string t = jstring_to_utf8(env, title);
    if (t.empty()) t = "Select Folder";

    GtkWidget* dlg = make_dialog(t, GTK_FILE_CHOOSER_ACTION_SELECT_FOLDER, "_Select");
    GtkFileChooser* chooser = GTK_FILE_CHOOSER(dlg);

    apply_initial_dir(chooser, env, initialDir);

    jstring result = nullptr;
    if (gtk_dialog_run(GTK_DIALOG(dlg)) == GTK_RESPONSE_ACCEPT) {
        char* fname = gtk_file_chooser_get_filename(chooser);
        if (fname) {
            result = utf8_to_jstring(env, fname);
            g_free(fname);
        }
    }
    gtk_widget_destroy(dlg);
    pump_events_until_destroyed(dlg);
    return result;
}

}
