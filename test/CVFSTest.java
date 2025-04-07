import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class CVFSTest {
    private CVFS cvfs;
    private CommandProcessor commandProcessor;
    private CommandTool commandTool;
    private ByteArrayOutputStream outputStreamCaptor;
    private PrintStream originalOut;
    private static final String TEST_FILE_PATH = "/Users/yinxuanjie/Desktop/COMP2021";

    //The test is done on the basis that the operating system is linux based,
    // preferably MaxOS as there will be different results.

    @Before
    public void setUp() {
        cvfs = new CVFS();
        commandProcessor = new CommandProcessor(cvfs);
        commandTool = new CommandTool();
        outputStreamCaptor = new ByteArrayOutputStream(); // 创建新的输出流实例
        originalOut = System.out; // 保存原始输出流
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
    }



    @Test
    public void testNewDisk() {
        commandProcessor.executeCommand("newDisk 100");
        VirtualDisk currentDisk = cvfs.getCurrentDisk();
        assertNotNull("Current disk should not be null", currentDisk);
    }

    @Test
    public void testNewDoc() {
        commandProcessor.executeCommand("newDoc doc1 txt HelloWorld");
        File file = cvfs.getWorkingDirectory().findFile("doc1");
        assertNotNull(file);
        assertTrue(file instanceof Document);
        Document doc = (Document) file;
        assertEquals("doc1", doc.getName());
        assertEquals("txt", doc.getType());
        assertEquals("HelloWorld", doc.getContent());
    }

    @Test
    public void testNewDir() {
        commandProcessor.executeCommand("newDir subDir");
        File file = cvfs.getWorkingDirectory().findFile("subDir");
        assertNotNull(file);
        assertTrue(file instanceof Directory);
        Directory dir = (Directory) file;
        assertEquals("subDir", dir.getName());
    }

    @Test
    public void testDeleteCommandValid() {
        commandProcessor.executeCommand("newDir file1");
        commandProcessor.executeCommand("delete file1");
        assertNull(cvfs.getWorkingDirectory().findFile("file1"));
    }

    @Test
    public void testDelete() {
        commandProcessor.executeCommand("doc1 txt ContentOfdoc1");
        cvfs.delete("doc1");
        File file = cvfs.getWorkingDirectory().findFile("doc1");
        assertNull(file);
    }

    @Test
    public void testRename() {
        commandProcessor.executeCommand("newDoc doc1 txt ContentOfdoc1");
        commandProcessor.executeCommand("rename doc1 doc2");
        File file = cvfs.getWorkingDirectory().findFile("doc1");
        assertNull(file);
        file = cvfs.getWorkingDirectory().findFile("doc2");
        assertNotNull(file);
        assertEquals("doc2", file.getName());
    }

    @Test
    public void testChangeDir() {
        commandProcessor.executeCommand("newDir subDir1");
        commandProcessor.executeCommand("changeDir subDir1");
        assertEquals("subDir1", cvfs.getWorkingDirectory().getName());
        commandProcessor.executeCommand("newDir subDir2");
        commandProcessor.executeCommand("changeDir $");
        assertEquals("root", cvfs.getWorkingDirectory().getName());
        commandProcessor.executeCommand("changeDir $/root/subDir1/subDir2");
        assertEquals("subDir2", cvfs.getWorkingDirectory().getName());
        commandProcessor.executeCommand("changeDir ..");
        assertEquals("subDir1", cvfs.getWorkingDirectory().getName());
    }

    @Test
    public void testList() {
        outputStreamCaptor.reset();
        commandProcessor.executeCommand("newDoc doc1 txt ContentOfdoc1");
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("list");
        String expectedOutput = "doc1(txt)66 bytes\n" +
                "dir1(Directory) 40 bytes\n" +
                "Total number of files/directory: 2\n" +
                "Total size: 106";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testRList() {
        commandProcessor.executeCommand("newDoc doc1 txt ContentOfdoc1");
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("changeDir dir1");
        commandProcessor.executeCommand("newDoc doc2 txt ContentOfdoc2");
        commandProcessor.executeCommand("changeDir $");
        commandProcessor.executeCommand("rList");
        String expectedOutput = "doc1 (txt) 66 bytes\n" +
                "dir1 (Directory) 106 bytes\n" +
                "\tdoc2 (txt) 66 bytes\n" +
                "Total number of files/directory: 2\n" +
                "Total size: 172";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testNewSimpleCri() {
        commandProcessor.executeCommand("newSimpleCri AB name contains \"doc\"");
        Criteria criteria = cvfs.searchCri("AB");
        assertNotNull(criteria);
        assertEquals("AB", criteria.getCriName());
    }

    @Test
    public void testNewBinaryCri() {
        commandProcessor.executeCommand("newSimpleCri A1 name contains \"doc\"");
        commandProcessor.executeCommand("newSimpleCri B1 type equals \"txt\"");
        commandProcessor.executeCommand("newBinaryCri C1 A1 && B1");
        Criteria criteria = cvfs.searchCri("C1");
        assertNotNull(criteria);
        assertEquals("C1", criteria.getCriName());
    }

    @Test
    public void testBinaryCriSearch() {
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newDir doc2");
        commandProcessor.executeCommand("newSimpleCri A1 name contains \"doc\"");
        commandProcessor.executeCommand("newSimpleCri B1 type equals \"txt\"");
        commandProcessor.executeCommand("newBinaryCri C1 A1 && B1");
        commandProcessor.executeCommand("newBinaryCri C2 A1 || B1");
        commandProcessor.executeCommand("search C1");
        commandProcessor.executeCommand("search C2");
        String expectedOutput = "doc1 (txt) 54 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 54 bytes\n" +
                "doc1 (txt) 54 bytes\n" +
                "doc2 (Directory) 40 bytes\n" +
                "Total files found: 2\n" +
                "Total size: 94 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testNewNegationCri() {
        commandProcessor.executeCommand("newDoc f1 txt content");
        commandProcessor.executeCommand("newSimpleCri A1 name contains \"doc\"");
        commandProcessor.executeCommand("newNegation B1 A1");
        commandProcessor.executeCommand("search B1");
        String expectedOutput = "f1 (txt) 54 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 54 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testIsDocument() {
        commandProcessor.executeCommand("newDoc doc1 txt doc1content");
        assertTrue("There should be IsDocument", cvfs.search("IsDocument"));
    }

    @Test
    public void testSimpleCri4(){
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newDoc doc2 txt content");
        commandProcessor.executeCommand("newSimpleCri aa type equals \"txt\"");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "doc1 (txt) 54 bytes\n" +
                "doc2 (txt) 54 bytes\n" +
                "Total files found: 2\n" +
                "Total size: 108 bytes";

        //assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSimpleCri5(){
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newSimpleCri aa size == 40");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "dir1 (Directory) 40 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 40 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSimpleCri6(){
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newSimpleCri aa size != 40");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "doc1 (txt) 54 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 54 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSimpleCri7(){
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newSimpleCri aa size > 40");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "doc1 (txt) 54 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 54 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSimpleCri8(){
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newSimpleCri aa size >= 40");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "dir1 (Directory) 40 bytes\n" +
                "doc1 (txt) 54 bytes\n" +
                "Total files found: 2\n" +
                "Total size: 94 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSimpleCri9(){
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newSimpleCri aa size < 40");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "Total files found: 0\n" +
                "Total size: 0 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSimpleCri10(){
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDoc doc1 txt content");
        commandProcessor.executeCommand("newSimpleCri aa size <= 40");
        commandProcessor.executeCommand("search aa");
        String expectedOutput = "dir1 (Directory) 40 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 40 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testPrintAllCriteria() {
        commandProcessor.executeCommand("newSimpleCri AB name contains \"doc\"");
        commandProcessor.executeCommand("newSimpleCri CD type equals \"Document\"");
        commandProcessor.executeCommand("newNegation EF AB");
        commandProcessor.executeCommand("newBinaryCri GH CD || AB");
        commandProcessor.executeCommand("printAllCriteria");

        String expectedOutput = "IsDocumentIsDocument: is Document \n" +
                "ABname: contains \"doc\" \n" +
                "CDtype: equals \"Document\" \n" +
                "EF!(name: contains \"doc\" )\n" +
                "GH(type: equals \"Document\"  || type: equals \"Document\" )";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testSearch() {
        commandProcessor.executeCommand("newDir dir1");
        commandProcessor.executeCommand("newDir doc2");
        commandProcessor.executeCommand("newSimpleCri AB name contains \"dir\"");
        commandProcessor.executeCommand("search AB");
        String expectedOutput = "dir1 (Directory) 40 bytes\n" +
                "Total files found: 1\n" +
                "Total size: 40 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    //有问题
    @Test
    public void testRSearch1() {
        commandProcessor.executeCommand("newDir Dir1");
        commandProcessor.executeCommand("changeDir Dir1");
        commandProcessor.executeCommand("newDir Dir2");
        commandProcessor.executeCommand("newSimpleCri AB name contains \"Dir\"");
        commandProcessor.executeCommand("changeDir $");
        commandProcessor.executeCommand("rSearch AB");
        String expectedOutput = "Dir1 (Directory) 80 bytes\n" +
                "\tDir2 (Directory) 40 bytes\n" +
                "Total number of file/directory: 1\n" +
                "Total size: 80 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }
    @Test
    public void testRSearch2() {
        commandProcessor.executeCommand("newDoc Doc1 txt content");
        commandProcessor.executeCommand("newDir Dir1");
        commandProcessor.executeCommand("changeDir Dir1");
        commandProcessor.executeCommand("newDoc Doc1 txt content");
        commandProcessor.executeCommand("newDir Dir2");
        commandProcessor.executeCommand("newSimpleCri AB name contains \"Doc\"");
        commandProcessor.executeCommand("changeDir $");
        commandProcessor.executeCommand("rSearch AB");
        String expectedOutput = "Doc1 (txt) 54 bytes\n" +
                "\tDoc1 (txt) 54 bytes\n" +
                "Total number of file/directory: 1\n" +
                "Total size: 54 bytes";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void testPathInitially() {
        assertEquals("$/root", cvfs.path());
    }

    @Test
    public void testChangeDirectory() {
        commandProcessor.executeCommand("newDir subDir1");
        commandProcessor.executeCommand("changeDir subDir1");
        assertEquals("$/root/subDir1", cvfs.path());
        commandProcessor.executeCommand("changeDir ..");
        assertEquals("$/root", cvfs.path());
    }



    public static boolean checkFileExists(String filePath) {
        java.io.File file = new java.io.File(filePath);
        return file.exists();
    }

    @Test
    public void testSave() {
        commandProcessor.executeCommand("save TEST_FILE_PATH");
        assertTrue(checkFileExists(TEST_FILE_PATH));
    }

    //Command Tool test
    @Test
    public void testInitialWorkingDirectory() {
        String expectedDir = "$/" + commandTool.getCvfs().getWorkingDirectory().getName();
        assertEquals(expectedDir, commandTool.getWorkingDirLabelText());
    }

    @Test
    public void testChangeDirectoryCommand() {
        commandTool.processCommand("changeDir ..");
        String expectedDir = "$/root";
        assertEquals(expectedDir, commandTool.getWorkingDirLabelText());
    }

    @Test
    public void NotEnoughSpaceDir() {
        commandProcessor.executeCommand("newDisk 2");
        commandProcessor.executeCommand("newDir dir1");

        String expectedOutput = "Not enough space to add directory.";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }

    @Test
    public void NotEnoughSpaceDoc() {
        commandProcessor.executeCommand("newDisk 2");
        commandProcessor.executeCommand("newDoc doc1 txt content");

        String expectedOutput = "Not enough space to add document.";
        assertEquals(expectedOutput, outputStreamCaptor.toString().trim());
    }



    // In order to make the exceptions report cleaner, we had to use
    // System.out.println to declare the exception thrown.
    // In this scenario, we aren't able to catch the exception thrown out properly.
    // Thus, we had to use flag to check if exception did occur.

    //test IllegalArgumentException

    @Test
    public void SearchCriNotFound() {
        commandProcessor.executeCommand("newNegation AA BB");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewDiskCommandWithInvalidLength() {
        commandProcessor.executeCommand("newDisk");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewDiskCommandWithInvalidSize() {
        commandProcessor.executeCommand("newDisk invalidSize");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewDocCommandWithInvalidLength() {
        commandProcessor.executeCommand("newDoc");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewDirCommandWithInvalidLength() {
        commandProcessor.executeCommand("newDir");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testDeleteCommandWithInvalidLength() {
        commandProcessor.executeCommand("delete");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testRenameCommandWithInvalidLength() {
        commandProcessor.executeCommand("rename");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testChangeDirCommandWithInvalidLength() {
        commandProcessor.executeCommand("changeDir");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testListCommandWithInvalidLength() {
        commandProcessor.executeCommand("list extraArg");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testRListCommandWithInvalidLength() {
        commandProcessor.executeCommand("rList extraArg");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewSimpleCriCommandWithInvalidLength() {
        commandProcessor.executeCommand("newSimpleCri");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewNegationCommandWithInvalidLength() {
        commandProcessor.executeCommand("newNegation");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testNewBinaryCriCommandWithInvalidLength() {
        commandProcessor.executeCommand("newBinaryCri");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testPrintAllCriteriaCommandWithInvalidLength() {
        commandProcessor.executeCommand("printAllCriteria extraArg");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testSearchCommandWithInvalidLength() {
        commandProcessor.executeCommand("search");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testRSearchCommandWithInvalidLength() {
        commandProcessor.executeCommand("rSearch");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testSaveCommandWithInvalidLength() {
        commandProcessor.executeCommand("save");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testLoadCommandWithInvalidLength() {
        commandProcessor.executeCommand("load");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testSimple1() {
        commandProcessor.executeCommand("newSimpleCri aa name contains this");
        assertTrue(commandProcessor.flag);
    }


    @Test
    public void testSimple2() {
        commandProcessor.executeCommand("newSimpleCri aa type contains \"this\"");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testSimple3() {
        commandProcessor.executeCommand("newSimpleCri aa size >> o");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testCriName() {
        commandProcessor.executeCommand("newSimpleCri aaaa name contains this");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testIsDocumentExtra() {
        commandProcessor.executeCommand("newSimpleCri IsDocument IsDocument is Document");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testAttrName() {
        commandProcessor.executeCommand("newSimpleCri aa vv contains \"this\"");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testSetName(){
        commandProcessor.executeCommand("newDoc abcdefghijk txt content");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testDocType(){
        commandProcessor.executeCommand("newDoc doc type Content");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testDuplication(){
        commandProcessor.executeCommand("newDoc doc txt content1");
        commandProcessor.executeCommand("newDoc doc txt content2");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testRename2(){
        commandProcessor.executeCommand("rename doc1 doc2");
        assertTrue(commandProcessor.flag);
    }

    @Test
    public void testSimple4(){
        commandProcessor.executeCommand("newSimpleCri aa size = 40");
        assertTrue(commandProcessor.flag);
    }


    //Quit will stop the test, which shows it is successful
//    @Test
//    public void testQuitCommand() {
//        try {
//            commandTool.processCommand("quit");
//        } catch (Exception e) {
//            fail("Exception thrown while processing quit command: " + e.getMessage());
//        }
//    }

}
