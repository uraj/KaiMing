package edu.psu.ist.plato.kaiming.test.x86.ir;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;

import org.junit.Assert;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;

import edu.psu.ist.plato.kaiming.x86.Function;
import edu.psu.ist.plato.kaiming.x86.ir.Printer;
import edu.psu.ist.plato.kaiming.x86.ir.Context;
import edu.psu.ist.plato.kaiming.x86.ir.AssemblyUnit;
import edu.psu.ist.plato.kaiming.x86.parsing.GASParser;
import edu.psu.ist.plato.kaiming.x86.parsing.ParsingException;


public class TestIRLifting {

    
    private List<Function> funs;
    private File testdir;
    
    public static String readFile(File path) 
            throws IOException 
    {
        byte[] encoded = Files.readAllBytes(path.toPath());
        return new String(encoded,     
                java.nio.charset.StandardCharsets.UTF_8);
    }
    
    @Before
    public void setup() {
        try {
            testdir = new File(getClass().getResource("/TestParser/").toURI());
        } catch (URISyntaxException e) {
            Assert.assertTrue("Test resources not found", false);
        }
        Assert.assertTrue(testdir.isDirectory());
        funs = new LinkedList<Function>();
        for (File f : testdir.listFiles()) {
            String content;
            try {
                content = readFile(f);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }
            try {
                funs.addAll(GASParser.parseBinaryUnitJava(content));
            } catch (ParsingException e) {
                e.printStackTrace();
            }
        }
    }
    
    @Test
    public void lift() {
        List<Context> ctxs = new ArrayList<Context>(funs.size());
        funs.forEach(f -> ctxs.add(new Context(f)));
        ctxs.forEach(c -> AssemblyUnit.UDAnalysis(c));
        ctxs.forEach(c -> Printer.out.printContextWithUDInfo(c));
    }
    
    @After
    public void cleanup() {
        
    }
}
