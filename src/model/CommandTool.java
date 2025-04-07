package hk.edu.polyu.comp.comp2021.cvfs.model;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.OutputStream;
import java.io.PrintStream;

public class CommandTool extends JFrame implements ActionListener {
    private CVFS cvfs;
    private JLabel workingDir;
    private JTextField textField;

    public CVFS getCvfs() {
        return cvfs;
    }

    public String getWorkingDirLabelText() {
        return workingDir.getText();
    }

    public void processCommand(String command) {
        textField.setText(command);
        ActionEvent event = new ActionEvent(textField, ActionEvent.ACTION_PERFORMED, command);
        for (ActionListener listener : textField.getActionListeners()) {
            listener.actionPerformed(event);
        }
    }

    public CommandTool() {
        this.cvfs = new CVFS();
        initUI();
    }

    private void initUI() {
        //The view of window
        setTitle("COMP2021 Group Project");
        setLayout(new BorderLayout());
        setSize(320, 320);
        setLocationRelativeTo(null);

        workingDir = new JLabel("$/"+ cvfs.getWorkingDirectory().getName());
        add(workingDir, BorderLayout.NORTH);

        textField = new JTextField();
        add(textField, BorderLayout.SOUTH);

        JTextArea outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);
        add(new JScrollPane(outputArea), BorderLayout.CENTER);

        //input of the user
        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String inputText = textField.getText();
                textField.setText("");
                outputArea.append(inputText+"\n");

                // Process the command & [REQ17] Command: quit
                if (inputText.equalsIgnoreCase("quit")) {
                    outputArea.append("Terminating the execution.\n");
                    System.exit(0);
                } else {
                    CommandProcessor cp = new CommandProcessor(cvfs);
                    cp.executeCommand(inputText);
                }
                if(inputText.startsWith("changeDir")){
                    workingDir.setText(cvfs.path());
                }
            }
        });

        PrintStream printStream = new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                outputArea.append(String.valueOf((char) b));
                outputArea.setCaretPosition(outputArea.getDocument().getLength());
            }
        });
        System.setOut(printStream);
        System.setErr(printStream);


        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
    }
    public void actionPerformed(ActionEvent event){};
}