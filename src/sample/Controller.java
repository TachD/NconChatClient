package sample;

import edu.BarSU.Ncon.Chat.Client.NcoNClient;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigInteger;
import java.net.*;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javafx.scene.input.KeyCode.ENTER;

public class Controller {
    private NcoNClient ClientObj = null;

    private volatile boolean NeedClose;

    private boolean Connectflag;

    private String HostName = null;

    @FXML
    private TextField HostNameField;

    @FXML
    private Tab PaneRegAuth;

    @FXML
    private Tab PaneAccount;

    @FXML
    private Tab PaneMessenger;

    @FXML
    private TextArea TAreaMain;

    @FXML
    private TextArea TAreaMessage;

    @FXML
    private Label StatusOff;

    @FXML
    private Label StatusOn;

    @FXML
    private TextField TFieldPORT;

    @FXML
    private Label BackToAutorisation;

    @FXML
    private Label RecoveryAccount;

    @FXML
    private Label GoToRegistration;

    @FXML
    private SplitPane RegAuthPane;

    // Recovery Data
    @FXML
    private Pane RecoveryPane;

    @FXML
    private TextField TFRecovery;

    @FXML
    private Label ToAutorisation;

    @FXML
    private Label LRecoveryStatus;

    // Registred data
    private String validMD5String;

    @FXML
    private Label ValidLabel;

    @FXML
    private TextField TFieldValidCode;

    @FXML
    private Button BSignUp;

    @FXML
    private Button AcceptRegistration;

    @FXML
    private Button CancelRegistration;

    @FXML
    private TextField RegNickname;

    @FXML
    private TextField RegFName;

    @FXML
    private TextField RegLname;

    @FXML
    private TextField RegEmail;

    @FXML
    private DatePicker RegDateofBirthday;

    @FXML
    private PasswordField RegPFieldOne;

    @FXML
    private PasswordField RegPFieldTwo;

    @FXML
    private Label StatusReg;

    // Autorisation data
    @FXML
    private TextField TFogin;

    @FXML
    private PasswordField PFPass;

    @FXML
    private Label LValidate;

    // Account data
    @FXML
    private Label LNickname;

    @FXML
    private Label LFirstname;

    @FXML
    private Label LLastname;

    @FXML
    private Label LEmail;

    @FXML
    private Label LDayofBirthday;

    /**
     *
     * @return time in "[HH.MM] " format
     */
    private String TimeLog() {
        LocalTime NowTime = LocalTime.now();

        String TimeLogMessage = "[";

        if (NowTime.getHour() < 10)
            TimeLogMessage += "0";

        TimeLogMessage += NowTime.getHour() + ":";

        if (NowTime.getMinute() < 10)
            TimeLogMessage += "0";

        TimeLogMessage += NowTime.getMinute() + "] ";

        return TimeLogMessage;
    }

    private String getEncryptedString(String SourceString) throws NoSuchAlgorithmException {
        final MessageDigest MD = MessageDigest.getInstance("SHA-256");

        MD.reset();
        MD.update(SourceString.getBytes(Charset.forName("UTF8")));

        return String.format("%064x", new BigInteger(1, MD.digest()));
    }

    private void MainMethod(int ServiceID) {
        Socket MainSocket = null;
        ObjectOutputStream OS = null;
        ObjectInputStream IS = null;

        if (!PaneRegAuth.isDisable()) {
            HostName = HostNameField.getText();
            HostNameField.setDisable(true);
        }

        try {
            SocketAddress SockAddr = new InetSocketAddress(InetAddress.getByName(HostName), 10001);

            MainSocket = new Socket();

            MainSocket.connect(SockAddr, 0);

            OS = new ObjectOutputStream(MainSocket.getOutputStream());

            OS.writeObject((ServiceID != 0)?ServiceID:TFieldPORT.getText());

            if (ServiceID < 0 && ServiceID > -5)
                IS = new ObjectInputStream(MainSocket.getInputStream());

            switch (ServiceID) {
                case -1:
                    Auth(OS, IS);
                    break;
                case -2:
                    Validation(OS, IS);
                    break;
                case -3:
                    Registration(OS, IS);
                    break;
                case -4:
                    Recovery(OS, IS);
                    break;
                case -5:
                    Logout(OS);
                    break;
                default:
                    ConnDisconn();
            }
        } catch (IOException IOEx) {
            System.out.println(IOEx.getMessage());
            HostName = null;
            HostNameField.setDisable(false);

            return;
        } finally {
            try {
                if (OS != null)
                    OS.close();

                if (IS != null)
                    IS.close();

                if (MainSocket != null || !MainSocket.isConnected())
                    MainSocket.close();
            } catch (IOException IOEx) {
                System.out.println(IOEx.getMessage());
                return;
            }
        }
    }

    private void Recovery(ObjectOutputStream OS, ObjectInputStream IS) {
        try {
            OS.writeObject(TFRecovery.getText());

            String RecoveryData;
            try {
                RecoveryData = IS.readObject().toString();
            } catch (Exception Ex) {
                return;
            }

            LRecoveryStatus.setText((Integer.valueOf(RecoveryData) == -1)?
                    "Unknown email!":"Check the mailbox!");

        } catch (IOException IOEx) {
            System.out.println("Server not found! " + IOEx);
        }
    }

    private void Auth(ObjectOutputStream OS, ObjectInputStream IS) {
        LValidate.setText("");

        try {
            OS.writeObject(TFogin.getText());

            try {
                OS.writeObject(getEncryptedString(PFPass.getText()));
            } catch (Exception Ex) {
                System.out.println("");
            }

            String LoginData;

            try {
                LoginData = IS.readObject().toString();
            } catch (Exception Ex) {
                return;
            }

            try {
                if (Integer.valueOf(LoginData) == 0)
                    LValidate.setText("Uncorrected Password and/or Login!");
            } catch (NumberFormatException NFEx) {
                try {

                    LNickname.setText(LoginData);
                    LEmail.setText(IS.readObject().toString());
                    LFirstname.setText(IS.readObject().toString());
                    LDayofBirthday.setText(IS.readObject().toString());
                    LLastname.setText(IS.readObject().toString());


                    PaneAccount.setDisable(false);
                    PaneMessenger.setDisable(false);
                    PaneRegAuth.setDisable(true);

                    TFogin.setText("");
                    PFPass.setText("");
                } catch (ClassNotFoundException CNFEx) {
                    System.out.println("Autorisation error!");
                }
            }

        } catch (IOException IOEx) {
            System.out.println("Not found server! " + IOEx);
        }
    }

    private void Validation(ObjectOutputStream OS, ObjectInputStream IS) {
        StatusReg.setText("");

        if (RegNickname.getText().length() < 3) {
            StatusReg.setText("Username is too short!");
            return;
        }

        if (!RegPFieldOne.getText().equals(RegPFieldTwo.getText())) {
            StatusReg.setText("Passwords do not match!");
            return;
        }

        if (RegPFieldOne.getText().length() < 5) {
            StatusReg.setText("Password is too short!");
            return;
        }

        Pattern EmailValiadtion =
                Pattern.compile("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

        Matcher matcher = EmailValiadtion.matcher(RegEmail.getText());

        if (!matcher.find()){
            StatusReg.setText("Uncorrected e-mail!");
            return;
        }

        if (RegFName.getText().length() < 2) {
            StatusReg.setText("Uncorrected first name!");
            return;
        }

        if (RegLname.getText().length() < 2) {
            StatusReg.setText("Uncorrected last name!");
            return;
        }

        int Year;

        try {
            Year = LocalDate.now().getYear() - RegDateofBirthday.getValue().getYear();
        } catch (Exception Ex) {
            return;
        }

        if (Year > 12) {
            StatusReg.setText("Uncorrected data of birthday!");
            return;
        }
        try {
            OS.writeObject(RegNickname.getText());
            OS.writeObject(RegFName.getText());
            OS.writeObject(RegLname.getText());
            OS.writeObject(RegEmail.getText());

            String RegData;
            try {
                 RegData = IS.readObject().toString();
            } catch (Exception Ex) {
                return;
            }

            try {
                if (Integer.valueOf(RegData) == -1)
                    StatusReg.setText("Nickname is already in use!");
                else
                    if (Integer.valueOf(RegData) == -2)
                        StatusReg.setText("Email is already in use!");

            } catch (NumberFormatException NFEx) {
                validMD5String = RegData;

                RegNickname.setEditable(false);
                RegPFieldOne.setEditable(false);
                RegPFieldTwo.setEditable(false);
                RegEmail.setEditable(false);
                RegFName.setEditable(false);
                RegDateofBirthday.setEditable(false);
                RegLname.setEditable(false);
                BSignUp.setDisable(true);


                ValidLabel.setVisible(true);
                AcceptRegistration.setVisible(true);
                TFieldValidCode.setVisible(true);
                CancelRegistration.setVisible(true);

                StatusReg.setText("Check the mailbox!");

            }

        } catch (IOException IOEx) {
            System.out.println("Server not found! " + IOEx);
        }
    }

    private void Registration(ObjectOutputStream OS, ObjectInputStream IS) {
        StatusReg.setText("");

        try {
            if (!validMD5String.equals(getEncryptedString(TFieldValidCode.getText()))) {
                StatusReg.setText("Incorrect code!");
                return;
            }
        } catch (NoSuchAlgorithmException NSAEx) {
            System.out.println("Validation error!");
            return;
        }

        try {
            String CryptPass;

            try {
                CryptPass = getEncryptedString(RegPFieldOne.getText());

            } catch (NoSuchAlgorithmException NSAEx) {
                System.out.println("Password encryption error! " + NSAEx.getMessage());
                return;
            }
            LocalDate Temp = RegDateofBirthday.getValue();

            String DateofBirth = new String();

            if (Temp.getDayOfMonth() < 10)
                DateofBirth += 0;
            DateofBirth += Temp.getDayOfMonth() + ".";

            if (Temp.getMonthValue() < 10)
                DateofBirth += 0;
            DateofBirth += Temp.getMonthValue() + "." + Temp.getYear();

            OS.writeObject(RegNickname.getText());
            OS.writeObject(CryptPass);
            OS.writeObject(RegFName.getText());
            OS.writeObject(RegLname.getText());
            OS.writeObject(RegEmail.getText());
            OS.writeObject(DateofBirth);
            ////////

            String LoginData;

            try {
                LoginData = IS.readObject().toString();
            } catch (Exception Ex) {
                return;
            }

                if (Integer.valueOf(LoginData) == 0)
                    StatusReg.setText("Account not created!");
                else {
                    StatusReg.setText("Account created!");

                    CancelRegistration();
                    RegAuthPane.setDividerPositions(0);
                }

        } catch (IOException IOEx) {
            System.out.println("Not found server! " + IOEx);
        }
    }

    public void Logout(ObjectOutputStream OS) {
        if (StatusOn.isVisible())
            CloseSession();

        try {
            OS.writeObject(LNickname.getText());
        } catch (IOException IOEx) {
            System.out.println(IOEx.getMessage());
        }

        LNickname.setText("");
        LEmail.setText("");
        LFirstname.setText("");
        LDayofBirthday.setText("");
        LLastname.setText("");

        PaneAccount.setDisable(true);
        PaneMessenger.setDisable(true);
        PaneRegAuth.setDisable(false);

        HostName = null;
        HostNameField.setDisable(false);
    }

    @FXML
    private void btnAuth() {
        MainMethod(-1);
    }

    @FXML
    private void btnValid() {
        MainMethod(-2);
    }

    @FXML
    private void btnReg() {
        MainMethod(-3);
    }

    @FXML
    private void btnRec() {
        MainMethod(-4);
    }

    @FXML
    private void btnLogout() {
        MainMethod(-5);
    }

    @FXML
    private void btnConnDisconn() {
        MainMethod(0);
    }

    @FXML
    private void CancelRegistration() {
        validMD5String = null;

        StatusReg.setText("");

        RegNickname.setEditable(true);
        RegNickname.setText("");

        RegPFieldOne.setEditable(true);
        RegPFieldOne.setText("");

        RegPFieldTwo.setEditable(true);
        RegPFieldTwo.setText("");

        RegEmail.setEditable(true);
        RegEmail.setText("");

        RegFName.setEditable(true);
        RegFName.setText("");

        RegDateofBirthday.setEditable(true);
        RegDateofBirthday.setValue(LocalDate.now());

        RegLname.setEditable(true);
        RegLname.setText("");

        BSignUp.setDisable(false);


        ValidLabel.setVisible(false);
        AcceptRegistration.setVisible(false);
        TFieldValidCode.setVisible(false);
        TFieldValidCode.setText("");
        CancelRegistration.setVisible(false);
    }
    /*
    private boolean ConnectionToServer(ObjectOutputStream OS) {
        int Port;

        try {
            Port = Integer.valueOf(TFieldPORT.getText());
        } catch (Exception Ex) {
            System.out.println("Port data uncorrected!");
            return false;
        }

        if (Port  > 10001 &&
            Port <= 65000) {
            try { // delete
                OS.writeObject(Port);
            } catch (Exception Ex) {
                System.out.println("Not found server! " + Ex);
                return false;
            }

            return true;
        }
        else
            return false;

    }
    */
    private boolean Prepare() {
        try {
            ClientObj = new NcoNClient(InetAddress.getByName(HostName), Integer.valueOf(TFieldPORT.getText()));
            return true;
        } catch (UnknownHostException UHEx) {
            System.out.println("Unknown host error! " + UHEx);
            return false;
        }
    }

    private void GetConnect() {
        //ConnectionToServer(OS); // edit or delete ?
        if (!Prepare())
            return;

        NeedClose = false;

        try {
            ClientObj.OpenStream();
        } catch (Exception Ex) {
            System.out.println("Opening streams error! " + Ex);
            return;
        }

        StatusOn.setVisible(true);
        StatusOff.setVisible(false);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Clip clip = null;

                try {

                    clip = AudioSystem.getClip();
                    AudioInputStream AIS = AudioSystem.getAudioInputStream(new File("src/source/sounds/notification.wav"));
                    clip.open(AIS);

                    while (true) {

                        if (NeedClose)
                            throw new Exception();

                        String Message = TimeLog() + " ";

                        Message += ClientObj.receive() + "\n\n";

                        TAreaMain.appendText(Message);

                        clip.start();
                        clip.setFramePosition(0);
                    }

                } catch (Exception Ex) {
                    StatusOn.setVisible(false);
                    StatusOff.setVisible(true);

                    if (clip != null)
                        clip.drain();

                    System.out.println((Ex.getMessage() == null)?"Session completed!":Ex.getMessage());

                    Connectflag = true;

                    if (ClientObj != null) {
                        ClientObj.CloseStream();
                        ClientObj = null;
                    }
                }
            }
        }).start();
    }

    private void CloseSession() {
        NeedClose = true;
    }

    private void ConnDisconn() {
        if (Connectflag) {
            GetConnect();

            Connectflag = false;
        }
        else {
            CloseSession();
            SendMessage();

            Connectflag = true;
        }
    }

    @FXML
    private void initialize()
            throws Exception {
        Connectflag = true;

        StatusOn.setVisible(false);
        StatusOff.setVisible(true);

        TAreaMessage.setOnKeyPressed(event -> {
            if (event.getCode().equals(ENTER))
                    SendMessage();
        });

        TAreaMessage.setOnKeyReleased(event -> {
            if (event.getCode().equals(ENTER))
                TAreaMessage.clear();
        });

        BackToAutorisation.setOnMouseMoved(event -> BackToAutorisation.setUnderline(true));

        BackToAutorisation.setOnMouseExited(event -> BackToAutorisation.setUnderline(false));

        BackToAutorisation.setOnMouseClicked(event -> RegAuthPane.setDividerPositions(100));

        GoToRegistration.setOnMouseMoved(event -> GoToRegistration.setUnderline(true));

        GoToRegistration.setOnMouseExited(event -> GoToRegistration.setUnderline(false));

        GoToRegistration.setOnMouseClicked(event -> RegAuthPane.setDividerPositions(0));

        RecoveryAccount.setOnMouseMoved(event -> RecoveryAccount.setUnderline(true));

        RecoveryAccount.setOnMouseExited(event -> RecoveryAccount.setUnderline(false));

        RecoveryAccount.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                RecoveryPane.setVisible(true);
                RecoveryPane.setDisable(false);
            }
        });

        ToAutorisation.setOnMouseMoved(event -> ToAutorisation.setUnderline(true));

        ToAutorisation.setOnMouseExited(event -> ToAutorisation.setUnderline(false));

        ToAutorisation.setOnMouseClicked(event -> {
            RecoveryPane.setVisible(false);
            RecoveryPane.setDisable(true);

            TFRecovery.setText("");
            LRecoveryStatus.setText("");
        });
    }

    @FXML
    private void SendMessage() {
        try {
            if (NeedClose)
                throw new IOException();

            if (ClientObj != null)
                if (TAreaMessage.getText().length() != 0)
                    if(ClientObj.isConnected()) {

                        String Message = TimeLog() + " ";
                        Message += LNickname.getText() + ": " + TAreaMessage.getText() + "\n\n";
                        TAreaMain.appendText(Message);

                        ClientObj.send(LNickname.getText() + ": " + TAreaMessage.getText());
                    }
        } catch (IOException IOEx) {
            StatusOn.setVisible(false);
            StatusOff.setVisible(true);

            if (ClientObj != null) {
                ClientObj.CloseStream();
                ClientObj = null;
            }

            System.out.println("Message sending error! " + IOEx);
            return;
    }
        TAreaMessage.clear();
    }
}
