package com.github.retrooper.packetevents.test.nbt;

import net.kyori.adventure.nbt.BinaryTag;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarFile;

/** Renames bytecode references, deliberately leaving reflection-name string constants unchanged. */
public final class NbtRenamingClassLoader extends ClassLoader {
    private static final String NBT_PREFIX = "net/kyori/adventure/nbt/";
    private static final String BRIDGE = "com/github/retrooper/packetevents/util/adventure/AdventureNbtUtil";
    private static final String PROBE = "com/github/retrooper/packetevents/test/nbt/RelocatedNbtProbe";
    private final Map<String, String> names = new HashMap<>();
    private final Map<String, String> originals = new HashMap<>();
    private final Remapper remapper;

    public NbtRenamingClassLoader(boolean obfuscate) throws Exception {
        super(NbtRenamingClassLoader.class.getClassLoader());
        try (JarFile jar = new JarFile(Paths.get(BinaryTag.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toFile())) {
            jar.stream().map(entry -> entry.getName())
                    .filter(name -> name.startsWith(NBT_PREFIX) && name.endsWith(".class"))
                    .sorted().forEach(name -> {
                        String original = name.substring(0, name.length() - 6);
                        String renamed = obfuscate ? "test/obfuscated/T" + names.size() : "test/shaded/" + original;
                        names.put(original, renamed);
                    });
        }
        names.put(BRIDGE, "test/shaded/Bridge");
        names.put(PROBE, "test/shaded/Probe");
        names.forEach((original, renamed) -> originals.put(renamed.replace('/', '.'), original));
        remapper = new Remapper() {
            @Override
            public String map(String internalName) {
                return names.getOrDefault(internalName, internalName);
            }

            @Override
            public String mapMethodName(String owner, String name, String descriptor) {
                if (obfuscate && owner.startsWith(NBT_PREFIX)
                        && ((name.equals("id") && descriptor.equals("()B"))
                        || (name.equals("read") && descriptor.equals("(Ljava/io/DataInput;)L" + NBT_PREFIX + "BinaryTag;"))
                        || (name.equals("write") && descriptor.equals("(L" + NBT_PREFIX + "BinaryTag;Ljava/io/DataOutput;)V")))) {
                    return "a"; // Like the production obfuscator, overload by descriptor.
                }
                return name;
            }

            @Override
            public String mapInvokeDynamicMethodName(String name, String descriptor) {
                // LambdaMetafactory's SAM name must follow the renamed Reader/Writer interface.
                String owner = Type.getReturnType(descriptor).getInternalName();
                if (obfuscate && owner.startsWith(NBT_PREFIX + "BinaryTagType")
                        && (name.equals("read") || name.equals("write"))) return "a";
                return name;
            }

            @Override
            public String mapFieldName(String owner, String name, String descriptor) {
                if (obfuscate && owner.startsWith(NBT_PREFIX)) {
                    if (name.equals("TYPES")) return "r";
                    if (name.equals("BYTE")) return "b";
                }
                return name;
            }
        };
    }

    public Class<?> renamedClass(String original) throws ClassNotFoundException {
        return loadClass(names.get(original.replace('.', '/')).replace('/', '.'));
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String original = originals.get(name);
        if (original == null) throw new ClassNotFoundException(name);
        try (InputStream stream = getParent().getResourceAsStream(original + ".class")) {
            if (stream == null) throw new ClassNotFoundException(original);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int count; (count = stream.read(buffer)) != -1;) bytes.write(buffer, 0, count);
            ClassWriter writer = new ClassWriter(0);
            new ClassReader(bytes.toByteArray()).accept(new ClassRemapper(writer, remapper), 0);
            byte[] transformed = writer.toByteArray();
            return defineClass(name, transformed, 0, transformed.length);
        } catch (Exception failure) {
            throw new ClassNotFoundException(name, failure);
        }
    }
}
