package hk.edu.polyu.comp.comp2021.cvfs;

import hk.edu.polyu.comp.comp2021.cvfs.model.CVFS;
import hk.edu.polyu.comp.comp2021.cvfs.model.CommandTool;

import javax.swing.*;

public class Application {

    // This program is written in MacOS system, if there are any issues feel free to contact us.

    public static void main(String[] args){
        CVFS cvfs = new CVFS();
        // Initialize and utilize the system
        SwingUtilities.invokeLater(CommandTool::new);
    }
}
