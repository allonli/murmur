__author__ = 'nekocode'
import os
import os.path

version_gradle = '1.2.3'
version_compile_sdk = '22'
version_build_tool = '22.0.1'
version_support_lib = '22.2.0'

def alter_root_gradle(path):
    try:
        f = open(path)
        text = f.read()
        index = text.index('gradle:') + 7
        text_new = text[:index] + version_gradle + text[index + 5:]
    finally:
        f.close()

    try:
        f = open(path, 'w')
        f.write(text_new)
    finally:
        f.close()


def alter_gradle(path):
    try:
        f = open(path)
        text = f.read()
        index = text.index('compileSdkVersion') + 17
        index2 = text.index('\n', index)
        index3 = text.index('buildToolsVersion') + 17
        index4 = text.index('\n', index3)
        text_new = text[:index] + ' ' + version_compile_sdk + text[index2:index3] + \
                   ' \"' + version_build_tool + '\"' + text[index4:]
    finally:
        f.close()

    try:
        f = open(path, 'w')
        f.write(text_new)
    finally:
        f.close()


nowdir = os.getcwd()
root_gradle = nowdir + '\\build.gradle'
alter_root_gradle(root_gradle)
print root_gradle + ' - done'

dirs = os.listdir(nowdir)
for filename in dirs:
    filename = nowdir + '/' + filename
    if os.path.isdir(filename):
        gradle = filename + '/build.gradle'
        if os.path.exists(gradle):
            alter_gradle(gradle)
            print gradle + ' - done'
