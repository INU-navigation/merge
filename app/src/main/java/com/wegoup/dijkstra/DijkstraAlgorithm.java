package com.wegoup.dijkstra;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.wegoup.incheon_univ_map.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DijkstraAlgorithm {
    private UndirectedGraph graph;
    private Context context;

    public DijkstraAlgorithm(UndirectedGraph graph, Context context) {
        this.graph = graph;
        this.context = context;
    }

    public List<String> findShortestPath(String startNode, String endNode) {
        Map<String, Double> distances = new HashMap();
        Map<String, String> previousNodes = new HashMap();
        Set<String> unvisitedNodes = new HashSet(this.graph.getAdjacencyList().keySet());
        Set<String> middleNodes = this.getMiddleNodesFromXML();
        unvisitedNodes.removeAll(middleNodes);
        unvisitedNodes.add(startNode);
        unvisitedNodes.add(endNode);
        Log.d("unvisited", unvisitedNodes.toString());
        distances.put(startNode, 0.0);

        label50:
        while(!unvisitedNodes.isEmpty()) {
            String currentNode = null;
            Iterator var8 = unvisitedNodes.iterator();

            while(true) {
                String node;
                do {
                    if (!var8.hasNext()) {
                        if (currentNode == null) {
                            break label50;
                        }

                        unvisitedNodes.remove(currentNode);
                        var8 = this.graph.getEdges(currentNode).iterator();

                        while(true) {
                            if (!var8.hasNext()) {
                                continue label50;
                            }

                            Edge edge = (Edge)var8.next();
                            String neighborNode = edge.endNode;
                            double edgeWeight = edge.length;
                            double totalDistance = (Double)distances.getOrDefault(currentNode, Double.MAX_VALUE) + edgeWeight;
                            if (totalDistance < (Double)distances.getOrDefault(neighborNode, Double.MAX_VALUE)) {
                                distances.put(neighborNode, totalDistance);
                                previousNodes.put(neighborNode, currentNode);
                            }
                        }
                    }

                    node = (String)var8.next();
                } while(currentNode != null && !((Double)distances.getOrDefault(node, Double.MAX_VALUE) < (Double)distances.getOrDefault(currentNode, Double.MAX_VALUE)));

                currentNode = node;
            }
        }

        List<String> shortestPath = new ArrayList();

        for(String currentNode = endNode; previousNodes.containsKey(currentNode); currentNode = (String)previousNodes.get(currentNode)) {
            shortestPath.add(currentNode);
        }

        shortestPath.add(startNode);
        Collections.reverse(shortestPath);
        return shortestPath;
    }

    private Set<String> getMiddleNodesFromXML() {
        Set<String> middleNodes = new HashSet();
        Resources res = this.context.getResources();
        String[] items = res.getStringArray(R.array.room_start);
        String[] var4 = items;
        int var5 = items.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            String item = var4[var6];
            if (!item.startsWith("194")) {
                middleNodes.add(item);
            }
        }

        return middleNodes;
    }

    public List<String> GetEdgeName(List<String> shortestPath) {
        List<String> pairedNodes = new ArrayList();

        for(int i = 0; i < shortestPath.size() - 1; ++i) {
            pairedNodes.add((String)shortestPath.get(i) + "," + (String)shortestPath.get(i + 1));
        }

        Log.d("노드짝 ", "노드짝: " + pairedNodes);
        List<String> edgeNames = new ArrayList();

        try {
            List<List<String>> pathCSV = CSVReader.pathCSV(this.context, "path12345-좌표최종2.csv");
            Iterator var5 = pairedNodes.iterator();

            while(true) {
                while(var5.hasNext()) {
                    String nodePair = (String)var5.next();

                    for(int i = 0; i < pathCSV.size(); ++i) {
                        String[] row = (String[])((List)pathCSV.get(i)).toArray(new String[0]);
                        if ((row[0] + "," + row[1]).equals(nodePair) || (row[1] + "," + row[0]).equals(nodePair)) {
                            edgeNames.add(row[2]);
                            break;
                        }
                    }
                }

                return edgeNames;
            }
        } catch (IOException var9) {
            var9.printStackTrace();
            return edgeNames;
        }
    }
}

