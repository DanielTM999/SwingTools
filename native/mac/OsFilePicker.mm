#import <Cocoa/Cocoa.h>
#include <jni.h>
#include <string>
#include <vector>
#include <sstream>

namespace {

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

NSString* nsstring_from_jstring(JNIEnv* env, jstring s) {
    if (!s) return nil;
    std::string utf = jstring_to_utf8(env, s);
    return [NSString stringWithUTF8String:utf.c_str()];
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

NSArray<NSString*>* extract_extensions(JNIEnv* env, jobjectArray specs) {
    NSMutableSet<NSString*>* exts = [NSMutableSet set];
    if (!specs) return @[];
    jsize count = env->GetArrayLength(specs);
    for (jsize i = 0; i < count; i++) {
        jstring js = static_cast<jstring>(env->GetObjectArrayElement(specs, i));
        std::string s = jstring_to_utf8(env, js);
        if (js) env->DeleteLocalRef(js);
        for (auto& pat : split(s, ';')) {
            std::string e = pat;
            auto pos = e.find_last_of('.');
            if (pos == std::string::npos) continue;
            std::string ext = e.substr(pos + 1);
            if (ext.empty() || ext == "*") return nil;
            [exts addObject:[NSString stringWithUTF8String:ext.c_str()]];
        }
    }
    return [exts allObjects];
}

void run_on_main(void (^block)(void)) {
    if ([NSThread isMainThread]) {
        block();
    } else {
        dispatch_sync(dispatch_get_main_queue(), block);
    }
}

}

extern "C" {

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFile(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    (void)filterNames;

    NSString* nsTitle = nsstring_from_jstring(env, title);
    NSString* nsDir = nsstring_from_jstring(env, initialDir);
    NSArray<NSString*>* exts = extract_extensions(env, filterSpecs);

    __block NSString* selected = nil;

    run_on_main(^{
        @autoreleasepool {
            NSOpenPanel* panel = [NSOpenPanel openPanel];
            if (nsTitle) panel.title = nsTitle;
            panel.canChooseFiles = YES;
            panel.canChooseDirectories = NO;
            panel.allowsMultipleSelection = NO;
            if (exts && exts.count > 0) panel.allowedFileTypes = exts;
            if (nsDir) panel.directoryURL = [NSURL fileURLWithPath:nsDir];
            if ([panel runModal] == NSModalResponseOK) {
                NSURL* url = panel.URLs.firstObject;
                if (url) selected = [url.path copy];
            }
        }
    });

    if (!selected) return nullptr;
    jstring out = utf8_to_jstring(env, [selected UTF8String]);
    [selected release];
    return out;
}

JNIEXPORT jobjectArray JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFiles(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    (void)filterNames;

    NSString* nsTitle = nsstring_from_jstring(env, title);
    NSString* nsDir = nsstring_from_jstring(env, initialDir);
    NSArray<NSString*>* exts = extract_extensions(env, filterSpecs);

    __block NSArray<NSString*>* selected = nil;

    run_on_main(^{
        @autoreleasepool {
            NSOpenPanel* panel = [NSOpenPanel openPanel];
            if (nsTitle) panel.title = nsTitle;
            panel.canChooseFiles = YES;
            panel.canChooseDirectories = NO;
            panel.allowsMultipleSelection = YES;
            if (exts && exts.count > 0) panel.allowedFileTypes = exts;
            if (nsDir) panel.directoryURL = [NSURL fileURLWithPath:nsDir];
            if ([panel runModal] == NSModalResponseOK) {
                NSMutableArray<NSString*>* arr = [NSMutableArray array];
                for (NSURL* u in panel.URLs) {
                    if (u.path) [arr addObject:u.path];
                }
                selected = [arr copy];
            }
        }
    });

    if (!selected) return nullptr;
    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray out = env->NewObjectArray(static_cast<jsize>(selected.count), strClass, nullptr);
    for (NSUInteger i = 0; i < selected.count; i++) {
        jstring js = utf8_to_jstring(env, [selected[i] UTF8String]);
        if (js) {
            env->SetObjectArrayElement(out, static_cast<jsize>(i), js);
            env->DeleteLocalRef(js);
        }
    }
    [selected release];
    return out;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nSaveFile(
        JNIEnv* env, jclass, jstring title, jstring initialDir, jstring defaultName,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    (void)filterNames;

    NSString* nsTitle = nsstring_from_jstring(env, title);
    NSString* nsDir = nsstring_from_jstring(env, initialDir);
    NSString* nsName = nsstring_from_jstring(env, defaultName);
    NSArray<NSString*>* exts = extract_extensions(env, filterSpecs);

    __block NSString* selected = nil;

    run_on_main(^{
        @autoreleasepool {
            NSSavePanel* panel = [NSSavePanel savePanel];
            if (nsTitle) panel.title = nsTitle;
            if (exts && exts.count > 0) panel.allowedFileTypes = exts;
            if (nsDir) panel.directoryURL = [NSURL fileURLWithPath:nsDir];
            if (nsName) panel.nameFieldStringValue = nsName;
            if ([panel runModal] == NSModalResponseOK) {
                NSURL* url = panel.URL;
                if (url) selected = [url.path copy];
            }
        }
    });

    if (!selected) return nullptr;
    jstring out = utf8_to_jstring(env, [selected UTF8String]);
    [selected release];
    return out;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFileOrDirectory(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    (void)filterNames;

    NSString* nsTitle = nsstring_from_jstring(env, title);
    NSString* nsDir = nsstring_from_jstring(env, initialDir);
    NSArray<NSString*>* exts = extract_extensions(env, filterSpecs);

    __block NSString* selected = nil;

    run_on_main(^{
        @autoreleasepool {
            NSOpenPanel* panel = [NSOpenPanel openPanel];
            if (nsTitle) panel.title = nsTitle;
            panel.canChooseFiles = YES;
            panel.canChooseDirectories = YES;
            panel.allowsMultipleSelection = NO;
            if (exts && exts.count > 0) panel.allowedFileTypes = exts;
            if (nsDir) panel.directoryURL = [NSURL fileURLWithPath:nsDir];
            if ([panel runModal] == NSModalResponseOK) {
                NSURL* url = panel.URLs.firstObject;
                if (url) selected = [url.path copy];
            }
        }
    });

    if (!selected) return nullptr;
    jstring out = utf8_to_jstring(env, [selected UTF8String]);
    [selected release];
    return out;
}

JNIEXPORT jobjectArray JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenFilesOrDirectories(
        JNIEnv* env, jclass, jstring title, jstring initialDir,
        jobjectArray filterNames, jobjectArray filterSpecs) {
    (void)filterNames;

    NSString* nsTitle = nsstring_from_jstring(env, title);
    NSString* nsDir = nsstring_from_jstring(env, initialDir);
    NSArray<NSString*>* exts = extract_extensions(env, filterSpecs);

    __block NSArray<NSString*>* selected = nil;

    run_on_main(^{
        @autoreleasepool {
            NSOpenPanel* panel = [NSOpenPanel openPanel];
            if (nsTitle) panel.title = nsTitle;
            panel.canChooseFiles = YES;
            panel.canChooseDirectories = YES;
            panel.allowsMultipleSelection = YES;
            if (exts && exts.count > 0) panel.allowedFileTypes = exts;
            if (nsDir) panel.directoryURL = [NSURL fileURLWithPath:nsDir];
            if ([panel runModal] == NSModalResponseOK) {
                NSMutableArray<NSString*>* arr = [NSMutableArray array];
                for (NSURL* u in panel.URLs) {
                    if (u.path) [arr addObject:u.path];
                }
                selected = [arr copy];
            }
        }
    });

    if (!selected) return nullptr;
    jclass strClass = env->FindClass("java/lang/String");
    jobjectArray out = env->NewObjectArray(static_cast<jsize>(selected.count), strClass, nullptr);
    for (NSUInteger i = 0; i < selected.count; i++) {
        jstring js = utf8_to_jstring(env, [selected[i] UTF8String]);
        if (js) {
            env->SetObjectArrayElement(out, static_cast<jsize>(i), js);
            env->DeleteLocalRef(js);
        }
    }
    [selected release];
    return out;
}

JNIEXPORT jstring JNICALL
Java_dtm_stools_component_inputfields_osfilepicker_OsFilePicker_nOpenDirectory(
        JNIEnv* env, jclass, jstring title, jstring initialDir) {
    NSString* nsTitle = nsstring_from_jstring(env, title);
    NSString* nsDir = nsstring_from_jstring(env, initialDir);

    __block NSString* selected = nil;

    run_on_main(^{
        @autoreleasepool {
            NSOpenPanel* panel = [NSOpenPanel openPanel];
            if (nsTitle) panel.title = nsTitle;
            panel.canChooseFiles = NO;
            panel.canChooseDirectories = YES;
            panel.allowsMultipleSelection = NO;
            if (nsDir) panel.directoryURL = [NSURL fileURLWithPath:nsDir];
            if ([panel runModal] == NSModalResponseOK) {
                NSURL* url = panel.URLs.firstObject;
                if (url) selected = [url.path copy];
            }
        }
    });

    if (!selected) return nullptr;
    jstring out = utf8_to_jstring(env, [selected UTF8String]);
    [selected release];
    return out;
}

}
