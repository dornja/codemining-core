/**
 *
 */
package codemining.java.codeutils.binding;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import codemining.java.tokenizers.JavaTokenizer;
import codemining.languagetools.ITokenizer;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

/**
 * Extract Java method bindings. Each method call or definition is used by
 * itself
 *
 * @author Miltos Allamanis <m.allamanis@ed.ac.uk>
 *
 */
public class JavaMethodInvocationBindingExtractor extends
		AbstractJavaNameBindingsExtractor {

	private static class MethodBindings extends ASTVisitor {
		/**
		 * A map from the method name to the position.
		 */
		Multimap<String, ASTNode> methodNamePostions = HashMultimap.create();

		@Override
		public boolean visit(final MethodInvocation node) {
			final String name = node.getName().toString();
			methodNamePostions.put(name, node.getName());
			return super.visit(node);
		}
	}

	private static void addImplementorVocab(final MethodInvocation method,
			Set<String> features) {
		ASTNode currentNode = method;
		List<String> tokenParts = null;
		while (currentNode.getParent() != null) {
			currentNode = currentNode.getParent();
			if (currentNode instanceof MethodDeclaration) {
				MethodDeclaration md = (MethodDeclaration) currentNode;
				tokenParts = getNameParts(md.getName().toString());
				break;
			} else if (currentNode instanceof TypeDeclaration) {
				TypeDeclaration td = (TypeDeclaration) currentNode;
				tokenParts = getNameParts(td.getName().toString());
				break;
			}
		}

		if (tokenParts != null) {
			for (final String tokenPart : tokenParts) {
				features.add("inName:" + tokenPart);
			}
		}
	}

	private static List<String> getNameParts(final String name) {
		List<String> nameParts = Lists.newArrayList();
		for (String snakecasePart : name.split("_")) {
			for (String w : snakecasePart
					.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])")) {
				nameParts.add(w.toLowerCase());
			}
		}
		return nameParts;
	}

	public JavaMethodInvocationBindingExtractor() {
		super(new JavaTokenizer());
	}

	public JavaMethodInvocationBindingExtractor(final ITokenizer tokenizer) {
		super(tokenizer);
	}

	@Override
	protected Set<String> getFeatures(final Set<ASTNode> boundNodes) {
		checkArgument(boundNodes.size() == 1);
		final ASTNode method = boundNodes.iterator().next().getParent();
		final Set<String> features = Sets.newHashSet();
		checkArgument(method instanceof MethodInvocation);
		final MethodInvocation mi = (MethodInvocation) method;
		features.add("nArgs:" + mi.arguments().size());
		addImplementorVocab(mi, features);
		JavaVariableFeatureExtractor.addAstFeatures(features, method);
		return features;
	}

	@Override
	public Set<Set<ASTNode>> getNameBindings(final ASTNode node) {
		final MethodBindings mb = new MethodBindings();
		node.accept(mb);

		final Set<Set<ASTNode>> nameBindings = Sets.newHashSet();
		for (final Entry<String, ASTNode> entry : mb.methodNamePostions
				.entries()) {
			final Set<ASTNode> boundNodes = Sets.newIdentityHashSet();
			boundNodes.add(entry.getValue());
			nameBindings.add(boundNodes);
		}
		return nameBindings;
	}

}
