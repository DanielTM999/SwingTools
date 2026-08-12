#ifndef STOOLS_GL_BINDINGS_H
#define STOOLS_GL_BINDINGS_H

#include <cstddef>

void* stgl_get_proc(const char* name);
bool stgl_load_functions();
void stgl_read_pixels(int width, int height, void* pixels);

#endif
