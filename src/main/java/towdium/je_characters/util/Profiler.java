package towdium.je_characters.util;

import com.google.gson.Gson;
import net.minecraftforge.common.MinecraftForge;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import towdium.je_characters.core.JechCore;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

/**
 * Author: Towdium
 * Date:   14/06/17
 */
public class Profiler {

    private static final JarContainer[] EMPTY_JC = new JarContainer[]{};
    private static final String[] EMPTY_STR = new String[]{};

    public static Report run() {
        File modDirectory = new File("mods");
        JarContainer[] jcs = scanDirectory(modDirectory).toArray(EMPTY_JC);
        Report r = new Report();
        r.jars = jcs;
        return r;
    }

    private static ArrayList<JarContainer> scanDirectory(File f) {
        File[] files = f.listFiles();
        ArrayList<JarContainer> jcs = new ArrayList<>();
        Consumer<JarContainer> callback = jcs::add;
        if (files != null) {
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".jar")) {
                    try (ZipFile mod = new ZipFile(file)) {
                        scanJar(mod, callback);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else if (file.isDirectory()) {
                    jcs.addAll(scanDirectory(file));
                }
            }
        }
        return jcs;
    }

    private static void scanJar(ZipFile f, Consumer<JarContainer> cbkJar) {
        ArrayList<String> methods = new ArrayList<>();
        Wrapper<ModContainer[]> mods = new Wrapper<>(null);
        Consumer<String> cbkMethod = methods::add;
        f.stream().forEach(entry -> {
            if (entry.getName().endsWith(".class")) {
                try (InputStream is = f.getInputStream(entry)) {
                    long size = entry.getSize() + 4;
                    if (size > Integer.MAX_VALUE) {
                        JechCore.LOG.info("Class file " + entry.getName()
                                + " in jar file " + f.getName() + " is too large, skip.");
                    } else {
                        scanClass(is, cbkMethod);
                    }
                } catch (IOException e) {
                    JechCore.LOG.info("Fail to read file " + entry.getName()
                            + " in jar file " + f.getName() + ", skip.");
                }
            } else if (entry.getName().equals("mcmod.info")) {
                Gson gson = new Gson();
                try (InputStream is = f.getInputStream(entry)) {
                    try {
                        mods.v = gson.fromJson(new InputStreamReader(is), ModContainer[].class);
                    } catch (Exception e) {
                        mods.v = new ModContainer[]{gson.fromJson(new InputStreamReader(is), ModContainer.class)};
                    }
                } catch (Exception e) {
                    JechCore.LOG.info("Fail to read mod info in jar file " + f.getName() + ", skip.");
                }
            }
        });
        if (!methods.isEmpty()) {
            JarContainer ret = new JarContainer();
            ret.methods = methods.toArray(EMPTY_STR);
            ret.mods = mods.v;
            cbkJar.accept(ret);
        }
    }

    private static void scanClass(InputStream is, Consumer<String> callback) throws IOException {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(is);
        try {
            classReader.accept(classNode, 0);
        } catch (Exception e) {
            if (classNode.name != null) {
                JechCore.LOG.info("File decoding of class " + classNode.name + " failed. Try to continue.");
            } else {
                throw new IOException(e);
            }
        }


        classNode.methods.forEach(methodNode -> {
            Iterator<AbstractInsnNode> it = methodNode.instructions.iterator();
            while (it.hasNext()) {
                AbstractInsnNode node = it.next();
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode mNode = ((MethodInsnNode) node);
                    if (mNode.getOpcode() == Opcodes.INVOKEVIRTUAL && mNode.owner.equals("java/lang/String")
                            && mNode.name.equals("contains") && mNode.desc.equals("(Ljava/lang/CharSequence;)Z")) {
                        callback.accept(classNode.name + ":" + methodNode.name + ":" + methodNode.desc);
                        break;
                    }
                }
            }
        });
    }

    @SuppressWarnings("unused")
    public static class Report {
        String version = "@VERSION@";
        String mcversion = MinecraftForge.MC_VERSION;
        String date;
        JarContainer[] jars;

        {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            date = sdf.format(new Date());
        }
    }

    private static class JarContainer {
        ModContainer[] mods;
        String[] methods;
    }

    @SuppressWarnings("unused")
    private static class ModContainer {
        String modid;
        String name;
        String version;
        String mcversion;
        String url;
        String[] authorList;
    }
}
