package com.ge.verdict.gsn;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** @author Saswata Paul */
public class Gsn2Dot {

    protected static List<GsnNode> allNodes = new ArrayList<>();

    public static void createDot(GsnNode GSN, File fout) throws IOException {

        System.out.println("Printing all node Ids:");
        getAllNodes(allNodes, GSN);
        for (GsnNode node : allNodes) {
            System.out.println(node.getNodeId());
        }

        System.out.println("Printing sample.dot:");

        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        bw.write("digraph G{");
        bw.newLine();
        writeNodes(allNodes, bw);
        bw.newLine();
        writeRelationships(allNodes, bw);
        bw.write("}");
        bw.close();
    }

    public static void getAllNodes(List<GsnNode> allNodes, GsnNode node) {

        if (node.getNodeType().equalsIgnoreCase("context")) {
            allNodes.add(node);
        } else if (node.getNodeType().equalsIgnoreCase("solution")) {
            allNodes.add(node);
            if (!(node.getInContextOf() == null)) {
                for (GsnNode subNode : node.getInContextOf()) {
                    getAllNodes(allNodes, subNode);
                }
            }
        } else if (node.getNodeType().equalsIgnoreCase("goal")) {
            allNodes.add(node);
            if (!(node.getInContextOf() == null)) {
                for (GsnNode subNode : node.getInContextOf()) {
                    getAllNodes(allNodes, subNode);
                }
            }
            if (!(node.getSupportedBy() == null)) {
                for (GsnNode subNode : node.getSupportedBy()) {
                    getAllNodes(allNodes, subNode);
                }
            }
        } else if (node.getNodeType().equalsIgnoreCase("strategy")) {
            allNodes.add(node);
            if (!(node.getInContextOf() == null)) {
                for (GsnNode subNode : node.getInContextOf()) {
                    getAllNodes(allNodes, subNode);
                }
            }
            if (!(node.getSupportedBy() == null)) {
                for (GsnNode subNode : node.getSupportedBy()) {
                    getAllNodes(allNodes, subNode);
                }
            }
        }
    }

    public static void writeNodes(List<GsnNode> allNodes, BufferedWriter bw) throws IOException {
        bw.write("//Declaring all nodes below");
        bw.newLine();
        for (GsnNode node : allNodes) {
            // String nodeDeclareString = "";
            String nodeShape = "";
            String nodeColor = "";
            String nodeText = "";

            if (node.getNodeType().equalsIgnoreCase("context")) {
                nodeShape = "rectangle, style=\"rounded\" ";
                nodeText = node.getContext().getDisplayText();
            } else if (node.getNodeType().equalsIgnoreCase("solution")) {
                nodeShape = "circle";
                nodeText = node.getSolution().getDisplayText();
            } else if (node.getNodeType().equalsIgnoreCase("goal")) {
                nodeShape = "box";
                nodeText = node.getGoal().getDisplayText();
            } else if (node.getNodeType().equalsIgnoreCase("strategy")) {
                nodeShape = "parallelogram";
                nodeText = node.getStrategy().getDisplayText();
            }

            String nodeDeclareString =
                    node.getNodeId()
                            + " ["
                            + "shape="
                            + nodeShape
                            + ", label=\""
                            + node.getNodeId()
                            + "\\n"
                            + nodeText
                            + "\"];";
            bw.write(nodeDeclareString);
            bw.newLine();
        }
    }

    public static void writeRelationships(List<GsnNode> allNodes, BufferedWriter bw)
            throws IOException {
        bw.write("//Declaring all node relationships below");
        bw.newLine();
        for (GsnNode node : allNodes) {
            for (GsnNode support : node.getSupportedBy()) {
                //				bw.write(node.getNodeId()+" -> "+ context.getNodeId()+" [constraint=false]");
                bw.write(node.getNodeId() + " -> " + support.getNodeId());
                bw.newLine();
            }
            for (GsnNode context : node.getInContextOf()) {
                //				bw.write(node.getNodeId()+" -> "+ context.getNodeId()+" [constraint=false]");
                bw.write(node.getNodeId() + " -> " + context.getNodeId());
                bw.newLine();
            }
        }
    }
}
