package io.ballerina.stdlib.http.compiler.staticcodeanalyzer;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NodeList;
import io.ballerina.projects.Document;
import io.ballerina.projects.plugins.AnalysisTask;
import io.ballerina.projects.plugins.SyntaxNodeAnalysisContext;
import io.ballerina.scan.Reporter;
import io.ballerina.stdlib.http.compiler.HttpCompilerPluginUtil;
import io.ballerina.tools.diagnostics.Location;

import static io.ballerina.stdlib.http.compiler.staticcodeanalyzer.HttpRule.AVOID_DEFAULT_RESOURCE_ACCESSOR;

public abstract class HttpServiceAnalyzer implements AnalysisTask<SyntaxNodeAnalysisContext> {
    private final Reporter reporter;

    public HttpServiceAnalyzer(Reporter reporter) {
        this.reporter = reporter;
    }

    public void validateServiceMembers(NodeList<Node> members, Document document) {
        // TODO: fix location, currently getting always -1 than expected
        HttpCompilerPluginUtil.getResourceMethodWithDefaultAccessor(members).forEach(definition -> {
            Location accessorLocation = definition.functionName().location();
            this.reporter.reportIssue(document, accessorLocation, AVOID_DEFAULT_RESOURCE_ACCESSOR.getId());
        });
    }
}
