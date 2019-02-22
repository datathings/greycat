/**
 * Copyright 2017-2019 The GreyCat Authors.  All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package greycat.rocksdb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LibraryLoader {

    private static final String ARM_EXTENSION = ".so";

    public static void loadArmLibrary(String fileName) {
        final String tmpDir = System.getenv("ROCKSDB_SHAREDLIB_DIR");
        if(tmpDir == null || tmpDir.isEmpty()) {
            throw new RuntimeException("Please set ROCKSDB_SHAREDLIB_DIR environment variable with a folder");
        }
        try {
            File tmpFile = new File(tmpDir,fileName + ARM_EXTENSION);
            tmpFile.createNewFile();
            tmpFile.deleteOnExit();

            try (final InputStream is = LibraryLoader.class.getClassLoader().getResourceAsStream("arm/" + fileName + ARM_EXTENSION)) {
                if (is == null) {
                    throw new RuntimeException(fileName + " was not found inside JAR.");
                } else {
                    Files.copy(is, tmpFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            System.load(tmpFile.getAbsolutePath());
        }catch (IOException e) {
            throw new RuntimeException("Error while loading library " + fileName + ".");
        }
    }
}
