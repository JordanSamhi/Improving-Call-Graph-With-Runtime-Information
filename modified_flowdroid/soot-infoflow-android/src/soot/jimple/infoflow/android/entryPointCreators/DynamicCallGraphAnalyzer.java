package soot.jimple.infoflow.android.entryPointCreators;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class DynamicCallGraphAnalyzer {

    private static DynamicCallGraphAnalyzer instance = null;

    private final Set<String> allMethods = new HashSet<>();
    private final Set<String> targetMethods = new HashSet<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<String, Set<String>> methodToComponents = new HashMap<>();
    private final Map<String, Set<String>> componentToEntryPoints = new HashMap<>();
    private Set<String> entryPoints = new HashSet<>();
    private Boolean isLoaded = false;

    private DynamicCallGraphAnalyzer() {
    }

    public static DynamicCallGraphAnalyzer v() {
        if (instance == null) {
            instance = new DynamicCallGraphAnalyzer();
        }
        return instance;
    }

    public void loadCallGraph(File jsonFile) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(jsonFile);
        JsonNode edgesNode = rootNode.get("edges");
        if (edgesNode != null && edgesNode.isArray()) {
            for (JsonNode edgeNode : edgesNode) {
                Edge edge = mapper.treeToValue(edgeNode, Edge.class);
                edges.add(edge);
                String sourceMethod = edge.getSourceMethod();
                String targetMethod = edge.getTargetMethod();
                if (sourceMethod != null) {
                    allMethods.add(sourceMethod);
                    List<String> components = edge.getComponent();
                    if (components != null) {
                        Set<String> componentSet = methodToComponents.getOrDefault(sourceMethod, new HashSet<>());
                        componentSet.addAll(components);
                        methodToComponents.put(sourceMethod, componentSet);
                    }
                }
                if (targetMethod != null) {
                    allMethods.add(targetMethod);
                    targetMethods.add(targetMethod);
                    List<String> components = edge.getComponent();
                    if (components != null) {
                        Set<String> componentSet = methodToComponents.getOrDefault(targetMethod, new HashSet<>());
                        componentSet.addAll(components);
                        methodToComponents.put(targetMethod, componentSet);
                    }
                }
            }
        }
        entryPoints = new HashSet<>(allMethods);
        entryPoints.removeAll(targetMethods);

        for (String entryPoint : entryPoints) {
            Set<String> components = methodToComponents.getOrDefault(entryPoint, Collections.emptySet());
            for (String component : components) {
                Set<String> entryPointSet = componentToEntryPoints.getOrDefault(component, new HashSet<>());
                entryPointSet.add(entryPoint);
                componentToEntryPoints.put(component, entryPointSet);
            }
        }

        this.isLoaded = true;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    public Set<String> getEntryPoints() {
        return entryPoints;
    }

    public Set<String> getMethodComponents(String method) {
        return methodToComponents.getOrDefault(method, Collections.emptySet());
    }

    public Map<String, Set<String>> getEntryPointComponents() {
        Map<String, Set<String>> entryPointComponents = new HashMap<>();
        for (String entryPoint : entryPoints) {
            Set<String> components = methodToComponents.getOrDefault(entryPoint, Collections.emptySet());
            entryPointComponents.put(entryPoint, components);
        }
        return entryPointComponents;
    }

    public Map<String, Set<String>> getComponentToEntryPoints() {
        return componentToEntryPoints;
    }

    public Set<String> getComponents() {
        return componentToEntryPoints.keySet();
    }

    public Set<String> getEntryPointsForComponent(String component) {
        return componentToEntryPoints.getOrDefault(component, Collections.emptySet());
    }

    public static class Edge {
        @JsonProperty("sourceMethod")
        private String sourceMethod;

        @JsonProperty("targetMethod")
        private String targetMethod;

        @JsonProperty("sourceStatement")
        private String sourceStatement;

        @JsonProperty("kind")
        private String kind;

        @JsonProperty("component")
        private List<String> component;

        public String getSourceMethod() {
            return sourceMethod;
        }

        public void setSourceMethod(String sourceMethod) {
            this.sourceMethod = sourceMethod;
        }

        public String getTargetMethod() {
            return targetMethod;
        }

        public void setTargetMethod(String targetMethod) {
            this.targetMethod = targetMethod;
        }

        public String getSourceStatement() {
            return sourceStatement;
        }

        public void setSourceStatement(String sourceStatement) {
            this.sourceStatement = sourceStatement;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public List<String> getComponent() {
            return component;
        }

        public void setComponent(List<String> component) {
            this.component = component;
        }
    }
}