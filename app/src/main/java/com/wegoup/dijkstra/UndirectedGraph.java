package com.wegoup.dijkstra;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class UndirectedGraph {
    private Map<String, List<Edge>> adjacencyList = new HashMap();

    public UndirectedGraph() {
    }

    public void addEdge(String startNode, String endNode, String edgeName, double length) {
        this.addEdgeToMap(startNode, endNode, edgeName, length);
        this.addEdgeToMap(endNode, startNode, edgeName, length);
    }

    private void addEdgeToMap(String startNode, String endNode, String edgeName, double length) {
        List<Edge> edges = (List)this.adjacencyList.getOrDefault(startNode, new ArrayList());
        edges.add(new Edge(startNode, endNode, edgeName, length));
        this.adjacencyList.put(startNode, edges);
    }

    List<Edge> getEdges(String node) {
        return (List)this.adjacencyList.getOrDefault(node, new ArrayList());
    }

    public String getGraphInfo() {
        StringBuilder builder = new StringBuilder();
        Iterator var2 = this.adjacencyList.keySet().iterator();

        while(var2.hasNext()) {
            String node = (String)var2.next();
            List<Edge> edges = (List)this.adjacencyList.get(node);
            Iterator var5 = edges.iterator();

            while(var5.hasNext()) {
                Edge edge = (Edge)var5.next();
                builder.append("StartNode: ").append(node).append(", EndNode: ").append(edge.endNode).append(", StringLineName: ").append(edge.edgeName).append(", Length: ").append(edge.length).append("\n");
            }
        }

        return builder.toString();
    }

    int getVertexCount() {
        return this.adjacencyList.size();
    }

    int getEdgeCount() {
        int count = 0;

        List edges;
        for(Iterator var2 = this.adjacencyList.values().iterator(); var2.hasNext(); count += edges.size()) {
            edges = (List)var2.next();
        }

        return count / 2;
    }

    public Map<String, List<Edge>> getAdjacencyList() {
        return this.adjacencyList;
    }

    private static class NodeDistancePair {
        String node;
        double distance;

        NodeDistancePair(String node, double distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
