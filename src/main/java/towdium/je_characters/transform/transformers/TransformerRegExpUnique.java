package towdium.je_characters.transform.transformers;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import towdium.je_characters.JECConfig;
import towdium.je_characters.transform.Transformer;

import java.util.Set;

/**
 * Author: Towdium
 * Date:   12/06/17
 */
public class TransformerRegExpUnique implements Transformer.Extended {
    MethodDecoder md = new MethodDecoder();

    public TransformerRegExpUnique() {
        md.addAll(JECConfig.EnumItems.ListDefaultRegExpMatch.getProperty().getStringList(), MethodDecoder.LOGGER);
        md.addAll(JECConfig.EnumItems.ListAdditionalRegExpMatch.getProperty().getStringList(), MethodDecoder.LOGGER);
        md.removeAll(JECConfig.EnumItems.ListMethodBlacklist.getProperty().getStringList(), MethodDecoder.LOGGER);
    }

    @Override
    public boolean accepts(String name) {
        return md.contains(name);
    }

    @Override
    public void transform(ClassNode n) {
        Set<String> methods = md.getMethodsForClass(n.name);
        if (!methods.isEmpty())
            n.methods.stream().filter(methodNode -> methods.contains(methodNode.name))
                    .forEach(methodNode -> Transformer.transformInvoke(
                            methodNode, "java/util/regex/Pattern", "matcher", "towdium/je_characters/CheckHelper",
                            "checkReg", "(Ljava/util/regex/Pattern;Ljava/lang/CharSequence;)Ljava/util/regex/Matcher;",
                            false, Opcodes.INVOKESTATIC, null, null
                    ));
    }
}
