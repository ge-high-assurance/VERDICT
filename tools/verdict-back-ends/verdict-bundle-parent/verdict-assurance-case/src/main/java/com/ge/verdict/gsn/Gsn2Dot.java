package com.ge.verdict.gsn;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** @author Saswata Paul */
public class Gsn2Dot {

    protected static List<GsnNode> allNodes = new ArrayList<>();

    public static void createDot(GsnNode GSN, File fout) throws IOException {

        getAllNodes(allNodes, GSN);

        FileOutputStream fos = new FileOutputStream(fout);
        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));

        bw.write("digraph G{");
        bw.newLine();
        bw.write("ratio = 0.5;");
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
        bw.write("//Node declarations:-");
        bw.newLine();
        for (GsnNode node : allNodes) {
            // String nodeDeclareString = "";
            String nodeShape = "";
            String nodeColor = "";
            String nodeText = "";
            String hoverDisplay = "No additional information is available.";
            boolean nodeStatus = true;

            //deciding node shape and hovertext
            if (node.getNodeType().equalsIgnoreCase("context")) {
                nodeShape = "rectangle, style=\"rounded\" ";
                nodeText = node.getContext().getDisplayText();
            	if (!(node.getContext().getExtraInfo()==null)) {
                	hoverDisplay = node.getContext().getExtraInfo();            		
            	}
            } else if (node.getNodeType().equalsIgnoreCase("solution")) {
                nodeShape = "circle";
                nodeText = node.getSolution().getDisplayText();
                nodeStatus = node.getSolution().getStatus();
            	if (!(node.getSolution().getExtraInfo()==null)) {
                	hoverDisplay = node.getSolution().getExtraInfo();            		
            	}
            } else if (node.getNodeType().equalsIgnoreCase("goal")) {
                nodeShape = "box";
                nodeText = node.getGoal().getDisplayText();
                nodeStatus = node.getGoal().getStatus();
            } else if (node.getNodeType().equalsIgnoreCase("strategy")) {
                nodeShape = "parallelogram";
                nodeText = node.getStrategy().getDisplayText();
                nodeStatus = node.getStrategy().getStatus();
            }

            //deciding node color
            if (nodeStatus) {
                nodeColor = "green";
            } else {
                nodeColor = "red";
            }
            if (node.getNodeType().equalsIgnoreCase("context")) {
                nodeColor = "black";
            }
            
//            //deciding hover text
//            if (node.getNodeType().equalsIgnoreCase("context")) {
//            	if (!(node.getContext().getExtraInfo()==null)) {
//                	hoverDisplay = node.getContext().getExtraInfo();            		
//            	}
//            } else if (node.getNodeType().equalsIgnoreCase("solution")) {
//            	if (!(node.getSolution().getExtraInfo()==null)) {
//                	hoverDisplay = node.getContext().getExtraInfo();            		
//            	}
//            } //else if (node.getNodeType().equalsIgnoreCase("goal")) {
//               
////            } else if (node.getNodeType().equalsIgnoreCase("strategy")) {
////            
////            }
            
            
            //writing string to declare node in dot file
            String nodeDeclareString =
                    node.getNodeId()
                            + " ["
                            + "tooltip=\""
                            + hoverDisplay
                            + "\", margin=0.05, style=bold, color="
                            + nodeColor
                            + ", width=1, shape="
                            + nodeShape
                            + ", label=\""
                            + node.getNodeId()
                            + "\\n\\n"
                            + nodeText
                            + "\"];";
            bw.write(nodeDeclareString);
            bw.newLine();
        }
    }

    public static void writeRelationships(List<GsnNode> allNodes, BufferedWriter bw)
            throws IOException {
        bw.write("//Node relationships (edges):-");
        bw.newLine();
        for (GsnNode node : allNodes) {
            for (GsnNode support : node.getSupportedBy()) {
                bw.write(
                        node.getNodeId()
                                + " -> "
                                + support.getNodeId()
                                + " [splines=curved, arrowsize=2.0]");
                bw.newLine();
            }
            for (GsnNode context : node.getInContextOf()) {
                bw.write(
                        node.getNodeId()
                                + " -> "
                                + context.getNodeId()
                                + " [splines=curved, arrowhead=empty, arrowsize=2.0]");
                bw.newLine();
            }
        }
    }
}
