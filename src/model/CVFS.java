package hk.edu.polyu.comp.comp2021.cvfs.model;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


// Base class for File - Documents & Directory
abstract class File implements Serializable{
    protected String name;

    public File(String name) {
        setName(name);
    }

    public void setName(String name) {
        //can only be alphanumeric and 10 characters long
        if(!name.matches("[a-zA-Z0-9]{1,10}"))
            throw new IllegalArgumentException("Name can only contain alphanumeric characters");

        this.name = name;
    }

    public String getName() { return name; }

    //implementation in Document and File
    public abstract int getSize();
}

// Document class
class Document extends File {
    private String type;
    private String content;
    private static final List<String> VALID_TYPES = List.of("txt", "java", "html", "css");

    public Document(String name, String type, String content) {
        super(name);

        //check if type is valid
        if(!VALID_TYPES.contains(type))
            throw new IllegalArgumentException("Invalid document type");

        this.type = type;
        this.content = content;
    }

    @Override
    public int getSize() {
        return 40 + content.length()*2;
    }

    public String getType() {
        return type;
    }

    public String getContent(){
        return content;
    }
}

// Directory class
class Directory extends File {
    private List<File> contents;

    public Directory(String name) {
        super(name);
        this.contents = new ArrayList<>();
    }

    public void addEntity(File entity){
        for(File existingEntity : contents){
            if(existingEntity.getName().equals(entity.getName())){
                throw new IllegalArgumentException("The filename has already existed");
            }
        }
        contents.add(entity);
    }


    public File findFile(String name){
        for (File entity : contents){
            if(entity.getName().equals(name)) {
                return entity;
            }
        }
        return null;
    }

    public Directory findParent(Directory curr){
        if(curr.getContents().contains(this)){
            return curr;
        }

        for(File entity: curr.getContents()){
            if(entity instanceof Directory dir){
                Directory parent = findParent(dir);
                if(parent != null){
                    return parent;
                }
            }
        }
        return null;
    }

    public void renameEntity(String fName, String newName){
        File file = findFile(fName);
        if(findFile(fName) == null)
            throw new IllegalArgumentException("File not found in the directory.");
        file.setName(newName);
    }

    public boolean removeEntity(String name) {
        boolean removed = contents.removeIf(entity -> entity.getName().equals(name));

        if(!removed){
            System.out.println("File not found in the directory.");
        }
        return removed;
    }

    @Override
    public int getSize() {
        int size = 40;

        //get all sizes of a document under the directory
        //iterate through all the files in the directory
        for (File entity : contents) {
            size += entity.getSize();
        }
        return size;
    }

    public List<File> getContents() {
        return contents;
    }

    //list command (REQ7)
    //list all files directly contained in the working directory
    public void list(){
        int fileNum = 0;
        long totalSize = 0;
        for(File entity : contents){

            if(entity instanceof Directory){
                System.out.println(entity.getName() + "(Directory) " + entity.getSize() + " bytes");
            }
            else if(entity instanceof Document doc){
                System.out.println(entity.getName() + "(" + doc.getType() + ")" + entity.getSize() + " bytes");
            }
            fileNum++;
            totalSize += entity.getSize();
        }
        System.out.println("Total number of files/directory: "+fileNum);
        System.out.println("Total size: "+ totalSize);
    }

    //rlist command (REQ8)
    //list all files contained in the working directory recursively
    //use indentation to indicate hierarchy
    public void rlist(){
        int level = 0;
        helper(level);
    }
    private void helper(int level){
        int fileNum = 0;
        int totalSize = 0;

        String indent = "\t".repeat(level);
        for (File entity : contents) {
            System.out.print(indent + entity.getName() + " ");

            if(entity instanceof Directory directory){
                System.out.println("(Directory) "  + entity.getSize() + " bytes");
                directory.helper(level + 1);
            }else if(entity instanceof Document doc){
                System.out.println("(" + doc.getType() + ") " + entity.getSize() + " bytes");
            }

            fileNum++;
            totalSize += entity.getSize();
        }

        if(level==0){
            System.out.println("Total number of files/directory: "+fileNum);
            System.out.println("Total size: "+ totalSize);
        }
    }
}

// VirtualDisk class
class VirtualDisk implements Serializable {
    private int maxSize;
    private Directory rootDirectory;
    private CriteriaManager critM;

    public VirtualDisk(int maxSize) {
        this.maxSize = maxSize;
        this.rootDirectory = new Directory("root");
        this.critM = new CriteriaManager("default");
    }

    public Directory getRootDirectory(){
        return rootDirectory;
    }


    public CriteriaManager getCriteriaManager(){
        return critM;
    }

    public boolean hasSpaceFor(int size){
        return (rootDirectory.getSize() + size) <= maxSize;
    }
}

//Criteria class
//(REQ9)
abstract class Criteria{
    protected String criName;
    protected CriteriaManager critM;

    public Criteria(){

    }

    public Criteria(String criName, CriteriaManager critM){

        if(criName.equals("IsDocument")){
        }
        else if(!criName.matches("[a-zA-Z0-9]{2}")){
            throw new IllegalArgumentException("Criteria name must contain exactly two letters.");
        }

        this.criName = criName;
        this.critM = critM;

    }

    public String getCriName(){
        return criName;
    }

    public abstract boolean evaluate(File file);
    public abstract String print();
}

class SimpleCriteria extends Criteria implements Serializable{
    private String attrName;
    private String op;
    private String val;

    public SimpleCriteria(){
        super();
    }

    public SimpleCriteria(String criName, String attrName, String op, String val, CriteriaManager critM){
        super(criName, critM);
        this.attrName = attrName;
        this.op = op;
        this.val = val;

        validate();
        critM.appendCri(this);
    }

    //check if command is correct
    public void validate(){
        if(attrName.equals("IsDocument")){
            if(!op.equals("is") || !val.equals("Document")){
                throw new IllegalArgumentException("Invalid criteria for IsDocument.");
            }
            return;
        }

        switch(this.attrName){
            case "name":
                if(!op.equals("contains")||!val.matches("^\".*\"$")){
                    throw new IllegalArgumentException("Invalid");
                }
                break;
            case "type":
                if(!op.equals("equals")||!val.matches("^\".*\"$")){
                    throw new IllegalArgumentException("Invalid");
                };
                break;
            case "size":
                if(!op.matches(">|<|>=|<=|==|!=")||!val.matches("\\d+")){
                    throw new IllegalArgumentException("Invalid criteria for size.");
                }
                break;
            default:
                throw new IllegalArgumentException("Invaild attribute name");
        }
    }

    //check if
    //[REQ 10]
    public boolean evaluate(File file){
        if(attrName.equals("IsDocument")){
            return file instanceof Document;
        }
        //newSimpleCriteria

        switch(attrName){
            case "name":
                return file.getName().contains(val.replace("\"",""));
            case "type":
                if(file instanceof Document doc) {
                    return doc.getType().equals(val.replace("\"",""));
                }
                else if(file instanceof Directory dir){
                    return dir.getClass().getSimpleName().equals(val.replace("\"",""));
                }
            case "size":
                int fileSize = file.getSize();
                int criteriaSize = Integer.parseInt(val);
                switch(op){
                    case ">":
                        return fileSize > criteriaSize;
                    case "<":
                        return fileSize < criteriaSize;
                    case ">=":
                        return fileSize >= criteriaSize;
                    case "<=":
                        return fileSize <= criteriaSize;
                    case "==":
                        return fileSize == criteriaSize;
                    case "!=":
                        return fileSize != criteriaSize;
                }
            default:
                throw new IllegalArgumentException("Invalid attribute name.");
        }
    }

    public String print(){
        return String.format("%s: %s %s %s", attrName, op, val, "");
    }
}

//[REQ11]  Command: newNegaLon / newBinaryCri
class NegationCriteria extends Criteria{
    private Criteria origCriteria;
    private String name;

    public NegationCriteria(String name, CriteriaManager critM, String origCriteria) {
        super(name, critM);
        this.origCriteria = critM.searchCri(origCriteria);
        critM.appendCri(this);
    }

    @Override
    public boolean evaluate(File file){
        return !origCriteria.evaluate(file);
    }

    @Override
    public String print(){
        return String.format("!(%s)", origCriteria.print());
    }
}

class BinaryCriteria extends Criteria{
    //private String criName;
    private Criteria critB;
    private Criteria critC;
    private String logicOp;

    public BinaryCriteria(String critA, String critB, String critC, CriteriaManager critM, String logicOp){
        super(critA, critM);
        this.critB = critM.searchCri(critB);
        this.critC = critM.searchCri(critC);
        this.logicOp = logicOp;

        critM.appendCri(this);
    }

    public boolean evaluate(File file){
        boolean resultB = critB.evaluate(file);
        boolean resultC = critC.evaluate(file);

        switch (logicOp){
            case "&&":
                return resultB && resultC;
            case "||":
                return resultB || resultC;
            default:
                throw new IllegalArgumentException("Invalid operation for binary criteria.");
        }
    }

    public String print(){
        return String.format("(%s %s %s)", critB.print(), logicOp, critB.print());
    }
}

//[REQ12]  Command: printAllCriteria
class CriteriaManager implements Serializable{
    private String cmName;
    private List<Criteria> criList;

    public CriteriaManager(String cmName) {
        this.cmName = cmName;
        this.criList = new ArrayList<>();


        new SimpleCriteria("IsDocument", "IsDocument", "is", "Document", this);
    }

    public void appendCri (Criteria criteria){
        for (Criteria existingCri : criList) {
            if (existingCri.getCriName().equals(criteria.getCriName())) {
                throw new IllegalArgumentException("Criteria name '" + criteria.getCriName() + "' already exists.");
            }
        }
        criList.add(criteria);
    }


    public void printAllCriteria(){
        for (Criteria c : criList) {
            System.out.println(c.getCriName() + c.print());
        }
    }

    public Criteria searchCri(String criName){
        Criteria crit = null;
        for (Criteria c : criList){
            if (c.getCriName().equals(criName)) {
                crit = c;
                break;
            }
        }

        if(crit == null){
            throw new IllegalArgumentException("Criteria not found.");
        }
        return crit;
    }

    //[REQ13]  Command: search criName
    public boolean searchDir(String criName, Directory workingDir) {
        Criteria criteria = searchCri(criName);

        //get workingDirectory
        List<File> files = workingDir.getContents();
        if (files == null){
            //note changes!
            throw new IllegalArgumentException("No file found in the working directory.");
        }

        long totalSize = 0;
        int count = 0;

        for (File file : files){
            if (criteria.evaluate(file)) {
                if(file instanceof Directory){
                    System.out.println(file.getName() + " (Directory) " + file.getSize() + " bytes");
                }
                else if(file instanceof Document doc){
                    System.out.println(file.getName() + " (" + doc.getType() + ") " + file.getSize() + " bytes");
                }

                totalSize += file.getSize();
                count++;
            }
        }

        System.out.println("Total files found: " + count);
        System.out.println("Total size: " + totalSize + " bytes");
        return true;
    }

    //[REQ14] Command: rsearch criName
    public void rSearch(String criName, Directory workingDir) {
        Criteria criteria = searchCri(criName);
        rSearchhelper(0, criteria, workingDir);
    }

    private void rSearchhelper(int level, Criteria criName, Directory workingDir) {
        int fileNum = 0;
        long totalSize = 0;

        List<File> contents = workingDir.getContents();
        if (contents == null || contents.isEmpty()) {
            return;
        }

        String indent = "\t".repeat(level);
        for (File entity : contents) {
            if(entity instanceof Document doc && criName.evaluate(entity)){
                System.out.println(indent + entity.getName() + " (" + doc.getType() + ") " + doc.getSize() + " bytes");
                fileNum++;
                totalSize += doc.getSize();
            } else if(entity instanceof Directory dir){
                if(criName.evaluate(dir)){
                    System.out.println(indent + entity.getName() + " (Directory) " + dir.getSize() + " bytes");
                    fileNum++;
                    totalSize += dir.getSize();
                }
                rSearchhelper(level + 1, criName, dir);
            }
        }
        if (level == 0) {
            System.out.println("Total number of file/directory: " + fileNum);
            System.out.println("Total size: " + totalSize + " bytes");
        }
    }
}

class CommandProcessor{
    public boolean flag = false;
    private CVFS fileSystem;

    public CommandProcessor(CVFS fileSystem){
        this.fileSystem = fileSystem;
    }

    public void executeCommand(String command) {
        String[] sCommand = command.split(" ");
        String commandName = sCommand[0];

        try{
            switch(commandName){
                case "newDisk":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid.");
                    }
                    int size;
                    try {
                        size = Integer.parseInt(sCommand[1]);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException("Invalid size for newDisk command.");
                    }
                    fileSystem.createDisk(size);
                    break;

                case "newDoc":
                    if (sCommand.length != 4){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.newDoc(sCommand[1], sCommand[2], sCommand[3]);
                    break;

                case "newDir":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.newDir(sCommand[1]);
                    break;

                case "delete":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.delete(sCommand[1]);
                    break;

                case "rename":
                    if (sCommand.length != 3){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.rename(sCommand[1], sCommand[2]);
                    break;

                case "changeDir":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.changeDir(sCommand[1]);
                    break;

                case "list":
                    if (sCommand.length != 1){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.list();
                    break;

                case "rList":
                    if (sCommand.length != 1){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.rList();
                    break;

                case "newSimpleCri":
                    if (sCommand.length != 5){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.newSimpleCri(sCommand[1], sCommand[2], sCommand[3], sCommand[4]);
                    break;

                case "newNegation":
                    if (sCommand.length != 3){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.newNegationCri(sCommand[1], sCommand[2]);
                    break;

                case "newBinaryCri":
                    if (sCommand.length != 5){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.newBinaryCri(sCommand[1], sCommand[2], sCommand[3], sCommand[4]);
                    break;

                case "printAllCriteria":
                    if (sCommand.length != 1){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.printAllCriteria();
                    break;

                case "search":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.search(sCommand[1]);
                    break;

                case "rSearch":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    fileSystem.rSearch(sCommand[1]);
                    break;

                case "save":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    saveVirDisk(sCommand[1]+".dat");
                    break;

                case "load":
                    if (sCommand.length != 2){
                        throw new IllegalArgumentException("Command invalid");
                    }
                    loadVirDisk(sCommand[1]+".dat");
                    break;

                default:
                    throw new IllegalArgumentException("Command invalid");
            }
        }

        catch (Exception e) {
            flag = true;
            System.out.print("Error: " + e.getMessage() + "\n");
        }
    }

    //save method
    private void saveVirDisk(String fPath){
        try{
            fileSystem.save(fPath);
            System.out.println("VirDisk saved to: " + fPath);
        }catch(IOException e){
            System.out.println("Saving Error: " + e.getMessage());
        }
    }

    //load method
    private void loadVirDisk(String fPath){
        try{
            fileSystem.load(fPath);
            System.out.println("VirDisk load from: " + fPath);
        }catch(IOException | ClassNotFoundException e){
            System.out.println("Loading Error: " + e.getMessage());
        }
    }
}


// CVFS class
public class CVFS implements Serializable{
    private VirtualDisk currentDisk;
    private Directory workingDirectory;
    private CriteriaManager critManager;

    public CVFS(){
        createDisk(1000);
        critManager = currentDisk.getCriteriaManager();
    }

    public void createDisk(int size){
        this.currentDisk = new VirtualDisk(size);
        this.workingDirectory = currentDisk.getRootDirectory();
    }

    public Directory getWorkingDirectory(){
        return workingDirectory;
    }

    public VirtualDisk getCurrentDisk(){
        return this.currentDisk;
    }


    public boolean changeDir(String name){
        if(name.charAt(0) == '$'){
            if(name.equals("$") || name.equals("$/root")) {
                this.workingDirectory = currentDisk.getRootDirectory();
                return true;
            }

            Directory currDir = currentDisk.getRootDirectory();
            String[] path = name.split("/");

            for(int i = 2; i < path.length; i++){
                File entity = currDir.findFile(path[i]);
                if(entity instanceof Directory dir) {
                    currDir = dir;
                    System.out.println(dir.getName());
                } else {
                    throw new IllegalArgumentException("Invalid directory.");
                }
            }
            System.out.println("HII 849");
            this.workingDirectory = currDir;

            return true;
        }


        if(name.equals( "..")){
            Directory parentDirectory = workingDirectory.findParent(currentDisk.getRootDirectory());
            if (parentDirectory != null) {
                this.workingDirectory = parentDirectory;
                System.out.println("Changed to parent directory: " + parentDirectory.getName());
            } else{
                System.out.println("Already at the root directory.");
            }
        }

        if(workingDirectory.findFile(name) instanceof Document || workingDirectory.findFile(name) == null){
            throw new IllegalArgumentException("Directory not found.");
        }
        this.workingDirectory = (Directory)workingDirectory.findFile(name);
        return true;
    }

    public boolean newDoc(String name, String type, String content) {
        Document doc = new Document(name, type, content);
        if (currentDisk.hasSpaceFor(doc.getSize())) {
            workingDirectory.addEntity(doc);
            return true;
        } else {
            System.out.println("Not enough space to add document.");
            return false;
        }
    }

    public boolean newDir(String name) {
        Directory dir = new Directory(name);
        if (currentDisk.hasSpaceFor(dir.getSize())) {
            workingDirectory.addEntity(dir);
            return true;
        } else {
            System.out.println("Not enough space to add directory.");
            return false;
        }
    }

    public void delete(String name) {
        workingDirectory.removeEntity(name);
    }

    public void rename(String oldName, String newName){
        workingDirectory.renameEntity(oldName, newName);
    }

    public void list(){
        workingDirectory.list();
    }

    public void rList(){
        workingDirectory.rlist();
    }

    public void newSimpleCri(String criName, String attrName, String op, String val){
        SimpleCriteria sCri = new SimpleCriteria(criName, attrName, op, val, critManager);
    }

    public void newNegationCri(String critA, String critB){
        NegationCriteria nCri = new NegationCriteria(critA, critManager, critB);
    }

    public void newBinaryCri(String critA, String critB, String op, String critC){
        BinaryCriteria bCri = new BinaryCriteria(critA, critB, critC, critManager, op);
    }


    public void printAllCriteria(){
        critManager.printAllCriteria();
    }

    public boolean search(String CriName){
        return critManager.searchDir(CriName, this.workingDirectory);
    }

    public void rSearch(String criName){
        critManager.rSearch(criName, this.workingDirectory);
    }

    public Criteria searchCri(String CriName){
        Criteria cri = critManager.searchCri(CriName);
        return cri;
    }

    public String path(){
        Directory root = currentDisk.getRootDirectory();
        return "$" + recPath(root, "");
    }

    public String recPath(Directory curr, String path){
        if(curr.equals(workingDirectory)){
            return "/" + curr.getName();
        }
        if(curr.getContents().contains(workingDirectory)) {
            return path + "/" + curr.getName() + "/" + workingDirectory.getName();
        }

        for(File entity: curr.getContents()){
            if(entity instanceof Directory dir) {
                String result = recPath(dir, path + "/" + curr.getName());
                if(result != null){
                    return result;
                }
            }
        }
        return null;
    }

    //[REQ 15] command: save
    public void save(String fPath) throws IOException{
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(fPath))) {
            oos.writeObject(this);  //serialization(object-->byte)
        }
    }

    //[REQ 16] command: load
    public void load(String fPath) throws IOException,ClassNotFoundException{
        try(ObjectInputStream ois = new ObjectInputStream(new FileInputStream(fPath))){
            CVFS load = (CVFS)ois.readObject(); //deserialization(byte-->object)
            this.currentDisk = load.currentDisk;
            this.workingDirectory = load.workingDirectory;
        }
    }
}