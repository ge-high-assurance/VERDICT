package com.ge.research.osate.verdict.gui;

import com.ge.research.osate.verdict.dsl.verdict.Statement;
import com.ge.research.osate.verdict.dsl.verdict.Verdict;
import com.ge.research.osate.verdict.dsl.verdict.VerdictContractSubclause;
import com.ge.research.osate.verdict.dsl.verdict.impl.CyberMissionImpl;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.resource.DefaultLocationInFileProvider;
import org.eclipse.xtext.util.ITextRegion;
import org.osate.aadl2.impl.DefaultAnnexSubclauseImpl;
import org.osate.aadl2.impl.SystemTypeImpl;

/** Author: Soumya Talukder Date: Jul 18, 2019 */

// this file updates the .aadl file with the content that user intends to save
// through wizard

public class ChangeInFile {

    private List<Integer> offsets = new ArrayList<Integer>();
    private List<Integer> lengths = new ArrayList<Integer>();
    private List<WzrdTableRow> deleteContents = new ArrayList<WzrdTableRow>();
    private File destinationFile;
    private IPath modelPath;
    private List<Boolean> missionTags = new ArrayList<Boolean>();

    public ChangeInFile(SystemTypeImpl systemInst, List<WzrdTableRow> list, IPath modelPath) {
        this.modelPath = modelPath;
        deleteContents = list;
        loadOffsetLength(getStatements(systemInst));
    }

    // create temporary copy of file
    private void copyFile(File source, File dest) throws IOException {
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(source);
            os = new FileOutputStream(dest);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        } finally {
            is.close();
            os.close();
        }
    }

    // extracts the existing annex-statements in the component "sys" on .aadl script
    private List<Statement> getStatements(SystemTypeImpl sys) {
        List<EObject> objs = sys.eContents();
        List<Statement> stmts = new ArrayList<Statement>();
        for (int i = 0; i < objs.size(); i++) {
            if (objs.get(i) instanceof DefaultAnnexSubclauseImpl) {
                if (!((DefaultAnnexSubclauseImpl) objs.get(i)).getName().equals("verdict")) {
                    continue;
                }
                Verdict vd =
                        ((VerdictContractSubclause)
                                        ((DefaultAnnexSubclauseImpl) objs.get(i))
                                                .getParsedAnnexSubclause())
                                .getContract();
                stmts = vd.getElements();
                break;
            }
        }
        return stmts;
    }

    // finds the offsets of annex statements as on the .aadl script
    private void loadOffsetLength(List<Statement> stmts) {
        DefaultLocationInFileProvider defLoc = new DefaultLocationInFileProvider();
        List<Integer> tmpOffset = new ArrayList<Integer>();
        List<Integer> tmpLength = new ArrayList<Integer>();
        for (int i = 0; i < stmts.size(); i++) {
            if (stmts.get(i) instanceof CyberMissionImpl) {
                ITextRegion region = defLoc.getFullTextRegion(stmts.get(i));
                tmpOffset.add(region.getOffset());
                tmpLength.add(region.getLength());
                missionTags.add(true);
                continue;
            }
            for (int j = 0; j < deleteContents.size(); j++) {
                if (stmts.get(i).getId().equals(deleteContents.get(j).getFormulaID())) {
                    missionTags.add(false);
                    ITextRegion region = defLoc.getFullTextRegion(stmts.get(i));
                    tmpOffset.add(region.getOffset());
                    tmpLength.add(region.getLength());
                }
            }
        }

        offsets = tmpOffset;
        offsets.sort(null);
        for (int i = 0; i < offsets.size(); i++) {
            for (int j = 0; j < tmpOffset.size(); j++) {
                if (offsets.get(i) == tmpOffset.get(j)) {
                    lengths.add(tmpLength.get(j));
                }
            }
        }
    }

    // updates file
    protected void refreshFile(
            int offset, boolean needToAppend, String appendix, Boolean systemLevel)
            throws IOException {

        updateOffsetLength(systemLevel);

        IPath ipath = modelPath.removeLastSegments(1);
        FileReader file =
                new FileReader(
                        ResourcesPlugin.getWorkspace()
                                .getRoot()
                                .getFile(modelPath)
                                .getRawLocationURI()
                                .toURL()
                                .getPath());
        String wrtpath =
                ResourcesPlugin.getWorkspace()
                        .getRoot()
                        .getFile(ipath.append("/tmp_tmp.aadl"))
                        .getRawLocationURI()
                        .toURL()
                        .getPath();
        FileWriter tmpFile = new FileWriter(wrtpath);

        int c;
        int i = 0;
        int count = 0;
        int delCount = 0;
        boolean startDeleting = false;

        while ((c = file.read()) != -1) {
            count++;
            if (i <= offsets.size() - 1) {
                if ((delCount < lengths.get(i)) && startDeleting) {
                    delCount++;
                    if (delCount == lengths.get(i)) {
                        startDeleting = false;
                        i++;
                    } else {
                        continue;
                    }
                }
                if (i <= offsets.size() - 1) {
                    if (count == offsets.get(i) + 1) {
                        delCount = 0;
                        startDeleting = true;
                        continue;
                    }
                }
            }
            if ((count == offset) && needToAppend) {
                char[] bufferTwo = new char[appendix.length()];
                appendix.getChars(0, appendix.length(), bufferTwo, 0);
                tmpFile.write(bufferTwo);
            }
            char repC = (char) c;
            char[] buffer = new char[1];
            String.valueOf(repC).getChars(0, 1, buffer, 0);
            tmpFile.write(buffer);
        }
        file.close();
        tmpFile.close();
        String newpath =
                ResourcesPlugin.getWorkspace()
                        .getRoot()
                        .getFile(modelPath)
                        .getRawLocationURI()
                        .toURL()
                        .getPath();
        File srcFile = new File(wrtpath);
        File fileDes = new File(newpath);
        copyFile(srcFile, fileDes);
        srcFile.delete();
    }

    // updates offset/length to account for space, tab and keyword
    private void updateOffsetLength(Boolean systemLevel) throws IOException {

        FileReader file =
                new FileReader(
                        ResourcesPlugin.getWorkspace()
                                .getRoot()
                                .getFile(modelPath)
                                .getRawLocationURI()
                                .toURL()
                                .getPath());
        int c;
        int i = 0;
        int count = 0;
        int delCount = 0;
        int tabCount = 0;
        int spaceCount = 0;
        boolean startDeleting = false;

        while ((c = file.read()) != -1) {
            count++;
            if (i <= offsets.size() - 1) {
                if ((delCount < lengths.get(i)) && startDeleting) {
                    delCount++;
                    if (delCount == lengths.get(i)) {
                        startDeleting = false;
                        i++;
                    } else {
                        continue;
                    }
                }
                if (i <= offsets.size() - 1) {
                    if (count == offsets.get(i) + 1) {
                        delCount = 1;
                        startDeleting = true;
                        if (!missionTags.get(i)) {
                            offsets.set(
                                    i,
                                    offsets.get(i)
                                            - 9
                                            - spaceCount
                                            - tabCount); // constant 9 is for keyword
                            // "CyberReq"/"CyberRel" and a \n
                            lengths.set(
                                    i,
                                    lengths.get(i)
                                            + 9
                                            + spaceCount
                                            + tabCount); // constant 9 is for keyword
                            // "CyberReq"/"CyberRel" and a \n
                        } else {
                            offsets.set(
                                    i,
                                    offsets.get(i)
                                            - 11
                                            - spaceCount
                                            - tabCount); // constant 11 is for keyword "MissionReq"
                            // and a \n
                            lengths.set(
                                    i,
                                    lengths.get(i)
                                            + 11
                                            + spaceCount
                                            + tabCount); // constant 11 is for keyword "MissionReq"
                            // and a \n
                        }
                        spaceCount = 0;
                        tabCount = 0;
                        continue;
                    }
                }
            }

            char repC = (char) c;
            if (String.valueOf(repC).equals("\n")) {
                tabCount = 0;
                spaceCount = 0;
            } else if (String.valueOf(repC).equals("\t")) {
                tabCount++;
            } else if (String.valueOf(repC).equals(" ")) {
                spaceCount++;
            }
        }
        file.close();
    }

    protected File getFile() {
        return destinationFile;
    }
}
