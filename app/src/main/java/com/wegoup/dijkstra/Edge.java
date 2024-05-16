package com.wegoup.dijkstra;

class Edge {
    String startNode;
    String endNode;
    String edgeName;
    double length;

    public String getEdgeName() {
        return this.edgeName;
    }

    Edge(String startNode, String endNode, String edgeName, double length) {
        this.startNode = startNode;
        this.endNode = endNode;
        this.edgeName = edgeName;
        this.length = length;
    }
}

