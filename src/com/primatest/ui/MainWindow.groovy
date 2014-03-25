package com.primatest.ui

import com.primatest.objectfinder.LookingGlass
import net.miginfocom.swing.MigLayout
import org.cyberneko.html.parsers.DOMParser
import org.gpl.JSplitButton.JSplitButton
import org.gpl.JSplitButton.action.SplitButtonActionListener
import org.xml.sax.InputSource


import javax.swing.ImageIcon
import javax.swing.JButton
import javax.swing.JCheckBoxMenuItem
import javax.swing.JComboBox
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenu
import javax.swing.JMenuBar
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.SwingWorker
import javax.swing.UIManager
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.WindowEvent
import java.awt.event.WindowListener

/**
 * Created with IntelliJ IDEA.
 * User: Dmitri
 * Date: 2/3/14
 * Time: 9:29 AM
 * To change this template use File | Settings | File Templates.
 */
//class MainWindow extends JFrame implements NativeKeyListener, WindowListener {
class MainWindow extends JFrame implements WindowListener {

    public LookingGlass glass
    DOMParser  parser = new DOMParser ()
    public def xpath =  XPathFactory.newInstance().newXPath()
    public String SelectedBrowser = "Internet Explorer"
    public JTextField idField
    public JButton pointerBtn
    public JTree DOMtree
    public DefaultMutableTreeNode rootNode
    public boolean lookingForObject = false
    public MainWindow mainWindow = this
    public uiToXMLHash = [:]
    public alreadyIncludedHash = [:]
    public def ShownElement = null
    public boolean autoSelect = false
    public JLabel infoLabel
    public JSplitButton performActionBtn
    public JComboBox idTypeList
    JCheckBoxMenuItem alwaysOnTop
    JMenuBar menubar
    JMenu fileMenu

    public MainWindow(){
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        setTitle("Looking Glass")
        setSize(800, 500)
        setLocationRelativeTo(null)
        setDefaultCloseOperation(EXIT_ON_CLOSE)
        addWindowListener(this)

        menubar = new JMenuBar();

        fileMenu = new JMenu("File")
        fileMenu.setMnemonic(KeyEvent.VK_F)

        JMenuItem eMenuItem = new JMenuItem("Exit")
        alwaysOnTop = new JCheckBoxMenuItem("Always On Top")
        alwaysOnTop.setSelected(true)
        eMenuItem.setMnemonic(KeyEvent.VK_E)
        eMenuItem.setToolTipText("Exit application")
        eMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        });

        alwaysOnTop.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if(isAlwaysOnTop()){
                    setAlwaysOnTop(false)
                }
                else{
                    setAlwaysOnTop(true)
                }
            }
        });

        fileMenu.add(alwaysOnTop)
        fileMenu.add(eMenuItem)

        menubar.add(fileMenu)

        JPanel mainPanel = new JPanel()
        JPanel midPanel = new JPanel()
        JPanel topPanel = new JPanel()
        JPanel botPanel = new JPanel()
        midPanel.setLayout(new MigLayout())
        mainPanel.setLayout(new MigLayout())
        topPanel.setLayout(new MigLayout())
        botPanel.setLayout(new MigLayout())
        add(mainPanel)


        String[] idStrings = ["XPath", "ID", "Name","CSS Selector","Class Name","Tag Name","Link Text","Partial Link Text"]
        JComboBox browserList = new browserTypeList()
        idTypeList = new JComboBox(idStrings)
        idTypeList.addActionListener (new ActionListener () {
            public void actionPerformed(ActionEvent e) {
                setIDValue()
            }
        });
        //idTypeList.setPreferredSize(new Dimension(130,20))

        performActionBtn = new JSplitButton("Validate  ")
        performActionBtn.addSplitButtonActionListener(new SplitButtonActionListener() {
            void buttonClicked(ActionEvent actionEvent) {
                if(performActionBtn.getText().startsWith("Validate")){
                    performElementAction("highlight")
                }
                else if(performActionBtn.getText().startsWith("Click")){
                    performElementAction("click")
                }
            }

            void splitButtonClicked(ActionEvent actionEvent) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        })
        JPopupMenu popupMenu = new JPopupMenu()

        JMenuItem clickItem = new JMenuItem("Click")
        clickItem.addActionListener(new ActionListener() {
            void actionPerformed(ActionEvent actionEvent) {
                performActionBtn.setText("Click ")
            }
        })

        JMenuItem validateItem = new JMenuItem("Validate")
        validateItem.addActionListener(new ActionListener() {
            void actionPerformed(ActionEvent actionEvent) {
                performActionBtn.setText("Validate ")
            }
        })

        JMenuItem typeItem = new JMenuItem("Type")
        typeItem.addActionListener(new ActionListener() {
            void actionPerformed(ActionEvent actionEvent) {
                performActionBtn.setText("Type ")
            }
        })


        popupMenu.add(validateItem)
        popupMenu.add(clickItem)
        popupMenu.add(typeItem)
        performActionBtn.setPopupMenu(popupMenu)
        //arrowBtn.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));


        idField = new JTextField(500)
        infoLabel = new JLabel("<html><font color=blue>Select Browser Type and click Open button.</font></html>")
        JButton startButn = new startBtn("Open")
        ImageIcon pointerBtnIcon = createImageIcon("images/find.png")
        ImageIcon copyBtnIcon = createImageIcon("images/copy.png")
        pointerBtn = new pointerBtn("",pointerBtnIcon)
        JButton copyBtn = new copyBtn("",copyBtnIcon)
        pointerBtn.setEnabled(false)
        rootNode = new DefaultMutableTreeNode(" <HTML>")
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        DOMtree = new JTree(treeModel)
        DOMtree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
        DOMtree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent e) {
                if(autoSelect == true) return
                DefaultMutableTreeNode node = (DefaultMutableTreeNode)DOMtree.getLastSelectedPathComponent()

                if (node == null) return
                def domnode = uiToXMLHash.find{it.value == node}
                if (domnode == null) return
                ShownElement = glass.getElementID(generateXPathFromDOM(domnode.key))
                setIDValue()
                //Object nodeInfo = node.getUserObject()
                //println nodeInfo
            }
        });
        DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) DOMtree.getCellRenderer();
        renderer.setLeafIcon(null);
        renderer.setClosedIcon(null);
        renderer.setOpenIcon(null);
        DOMtree.setRootVisible(true);
        DOMtree.setShowsRootHandles(true);
        JScrollPane treeView = new JScrollPane(DOMtree);

        topPanel.add(browserList)
        topPanel.add(startButn)
        topPanel.add(pointerBtn)

        midPanel.add(idTypeList,"growx")
        midPanel.add(idField, "grow")
        midPanel.add(copyBtn)
        midPanel.add(performActionBtn)


        mainPanel.add(topPanel,"wrap")
        mainPanel.add(infoLabel,"span, grow, wrap")
        mainPanel.add(midPanel,"wrap")
        mainPanel.add(treeView,"span, grow,height 200::")
        //mainPanel.add(botPanel,"span, grow,height 200::")
        //basic.add(topPanel)

        setJMenuBar(menubar)
        setAlwaysOnTop(true)
    }

    protected static ImageIcon createImageIcon(String path) {
        //println System.getProperty("user.dir")+path
        //return new ImageIcon(imgURL);
        return new ImageIcon(System.getProperty("user.dir")+"/"+path);
    }


    def stopRecording(){
        setState(NORMAL)
        if(!isAlwaysOnTop()){
            setAlwaysOnTop(true)
            setAlwaysOnTop(false)
        }
        Robot robot = new Robot()
        sleep(200)
        def mouseLocation = MouseInfo.getPointerInfo().getLocation()
        robot.mouseMove(getX()+50, getY()+20)
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
        robot.mouseMove(mouseLocation.x.toInteger(), mouseLocation.y.toInteger())
        glass.stopRecording()
        fileMenu.setSelected(false)
        idField.requestFocus()
        infoLabel.setText("<html><font color=blue>Click on the looking glass and move mouse pointer to html element.</font></html>")
        performActionBtn.setEnabled(true)
    }

    /*
    def ctrlPressed = false
    def altPressed = false
    void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
        if (nativeKeyEvent.getKeyCode() == NativeKeyEvent.VK_CONTROL) {
            ctrlPressed = true
        }
        else if (nativeKeyEvent.getKeyCode() == NativeKeyEvent.VK_ALT) {
            altPressed = true
        }
        if(altPressed && ctrlPressed){
            try{
                if(lookingForObject){
                    lookingForObject = false
                    setState(NORMAL)
                    if(!isAlwaysOnTop()){
                        setAlwaysOnTop(true)
                        setAlwaysOnTop(false)
                    }
                    Robot robot = new Robot()
                    def mouseLocation = MouseInfo.getPointerInfo().getLocation()
                    robot.mouseMove(getX()+10, getY()+10)
                    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
                    robot.mouseMove(mouseLocation.x.toInteger(), mouseLocation.y.toInteger())
                    glass.stopRecording()
                    fileMenu.setSelected(false)
                    idField.requestFocus()
                    infoLabel.setText("<html><font color=blue>Click on the looking glass and move mouse pointer to html element.</font></html>")
                    performActionBtn.setEnabled(true)
                }
            }
            catch (Exception ex){
                println ex.message
            }
        }
    }
    void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {
        if (nativeKeyEvent.getKeyCode() == NativeKeyEvent.VK_CONTROL) {
            ctrlPressed = false
        }
        else if (nativeKeyEvent.getKeyCode() == NativeKeyEvent.VK_ALT) {
            altPressed = false
        }
    }

    void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {}
    */

    public void windowOpened(WindowEvent e) {
    }

    void windowClosing(WindowEvent windowEvent) {}

    public void windowClosed(WindowEvent e) {
        System.runFinalization();
    }

    void windowIconified(WindowEvent windowEvent) {}

    void windowDeiconified(WindowEvent windowEvent) {}

    void windowActivated(WindowEvent windowEvent) {}

    void windowDeactivated(WindowEvent windowEvent) {}

    class copyBtn extends JButton implements ActionListener {
        public copyBtn(String text,ImageIcon icon) {
            super.setText(text)
            super.setIcon(icon)
            addActionListener(this)
        }

        void actionPerformed(ActionEvent actionEvent) {
            Clipboard clpbrd = Toolkit.getDefaultToolkit ().getSystemClipboard ();
            clpbrd.setContents (new StringSelection (idField.getText()),null);
        }
    }

    class pointerBtn extends JButton implements ActionListener {
        class ObjectLocator extends SwingWorker<String, Object> {
            private MainWindow main
            ObjectLocator(MainWindow mainWindow) {
                main = mainWindow
                main.parseHTML(main.glass.getHTML())
                //main.infoLabel.setText("<html><font color=blue>Hit CTRL+ALT to stop element tracking.</font></html>")
                main.infoLabel.setText("<html><font color=blue>Click on any element to stop tracking.</font></html>")
                main.performActionBtn.setEnabled(false)
            }

            @Override
            public String doInBackground() {
                while(main.lookingForObject){
                    def chunk = main.glass.FindObject({main.parseHTML(main.glass.getHTML())})
                    publish(chunk)
                }
                return ""
            }

            @Override
            protected void process(List<Object> chunks) {
                for (Object id : chunks) {
                    if(id == "END OF RECORDING"){
                        main.lookingForObject = false;
                        stopRecording()
                        return
                    }
                    main.ShownElement = id
                    main.setIDValue()
                    //main.idField.setText(id.xpath)
                    //main.idField.setText(id.css)
                    main.autoSelect = true
                    main.selectUINodeByXpath(id.xpath)
                    main.autoSelect = false
                }
            }
        }


        public pointerBtn(String text,ImageIcon icon) {
            super.setText(text)
            super.setIcon(icon)
            addActionListener(this)
        }
        public void actionPerformed(ActionEvent e) {
            lookingForObject = true
            (new ObjectLocator(mainWindow)).execute();
        }
    }

    class startBtn extends JButton implements ActionListener {

        public startBtn(String text) {
            super.setText(text)
            addActionListener(this)
        }

        public void actionPerformed(ActionEvent e) {
            this.setEnabled(false)
            this.setText("Opening...")
            def exceptionClosure = {ex ->
                JOptionPane.showMessageDialog(mainWindow,
                        "Error starting browser: "+ex.message,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                this.setEnabled(true)
                this.setText("Open")
            }
            glass = new LookingGlass({pointerBtn.setEnabled(true);this.setEnabled(true);this.setText("Open")},exceptionClosure)
            infoLabel.setText("<html><font color=blue>Click on the looking glass and move mouse pointer to html element.</font></html>")
            glass.BrowserType = SelectedBrowser
            //glass.start()
            Thread t = new Thread(glass);
            t.start();

        }
    }

    def performElementAction(String actionType){
        if(glass == null) return
        performActionBtn.setEnabled(false)
        infoLabel.setText("<html><font color=blue>Click on the looking glass and move mouse pointer to html element.</font></html>")
        def response = glass.performElementAction(idField.getText(),{parseHTML(glass.getHTML())},idTypeList.getSelectedItem(),actionType)
        if(response.text != ""){
            infoLabel.setText("<html>$response.text</html>")
        }
        autoSelect = true
        selectUINodeByXpath(response.xpath)
        autoSelect = false
        performActionBtn.setEnabled(true)
    }

    def setIDValue(){
        if(idTypeList.getSelectedItem() == "XPath"){
            idField.setText(ShownElement.xpath)
        }
        else if(idTypeList.getSelectedItem() == "ID"){
            idField.setText(ShownElement.id)
        }
        else if(idTypeList.getSelectedItem() == "Name"){
            idField.setText(ShownElement.name)
        }
        else if(idTypeList.getSelectedItem() == "CSS Selector"){
            idField.setText(ShownElement.css)
        }
        else if(idTypeList.getSelectedItem() == "Class Name"){
            idField.setText(ShownElement.className)
        }
        else if(idTypeList.getSelectedItem() == "Tag Name"){
            idField.setText(ShownElement.tagName)
        }
        else if(idTypeList.getSelectedItem() == "Link Text"){
            idField.setText(ShownElement.linkText)
        }
        else if(idTypeList.getSelectedItem() == "Partial Link Text"){
            idField.setText(ShownElement.linkText)
        }
        //["XPath", "ID", "Name","CSS Selector","Class Name","Tag Name","Link Text","Partial Link Text"]

    }

    def addNodes(domnode,uinode){
        if(domnode.class == org.apache.xerces.dom.TextImpl) {
            def trimmedData = domnode.data.trim()
            trimmedData = trimmedData.replace("\n","")
            if(trimmedData != ""){
                if((alreadyIncludedHash.get(domnode) != null)&&(alreadyIncludedHash.get(domnode) == false)){
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(domnode.data)
                    uinode.add(node)
                }
                else if(alreadyIncludedHash.get(domnode) == null){
                    DefaultMutableTreeNode node = new DefaultMutableTreeNode(domnode.data)
                    uinode.add(node)
                }
            }
            return
        }
        if(domnode.class == org.apache.xerces.dom.CommentImpl) return
        //def uiText = "  <"+domnode.getTagName()
        def uiText = "<html><font color=C700FF> &lt;${domnode.getTagName()}</font>"
        def attributes = ""
        if(domnode.getAttributes() != null){
            domnode.getAttributes().nodes.each{
                def value = org.apache.commons.lang3.StringEscapeUtils.escapeHtml4(it.value)
                attributes = attributes + " " + "<font color=FF8A13>$it.name=</font><font color=C700FF>\"</font><font color=blue>$value</font><font color=C700FF>\"</font>"
            }
        }

        if((domnode.firstChild != null)&&(domnode.firstChild.class == org.apache.xerces.dom.TextImpl)){
            //domnode.firstChild.metaClass.includedAlready = false
            alreadyIncludedHash.put(domnode.firstChild,false)
            def trimmedData = domnode.firstChild.data.trim()
            trimmedData = trimmedData.replace("\n","")
            if ((trimmedData != "")&&(domnode.getChildNodes().length == 1)){
                //uiText = uiText + attributes+">"+domnode.firstChild.data+"</${domnode.getTagName()}>"
                uiText = uiText + attributes+"<font color=C700FF>&gt;</font>"+domnode.firstChild.data+"<font color=C700FF>&lt/${domnode.getTagName()}</font><font color=C700FF>&gt;</font></html>"
                //domnode.firstChild.metaClass.includedAlready = true
                alreadyIncludedHash.put(domnode.firstChild,true)
            }
            else if((domnode.getChildNodes().length == 1)&&(trimmedData != "")){
                //uiText = "  <"+domnode.getTagName()+attributes+"/>"
                uiText = "  <font color=C700FF>&lt;"+domnode.getTagName()+"</font>"+attributes+"<font color=C700FF>/&gt;</font></html>"
            }
            else{
                uiText = uiText+attributes+"<font color=C700FF>&gt;</font></html>"
            }
        }
        else if(domnode.getChildNodes().length == 0){
            if(attributes == ""){
                uiText = uiText+"<font color=C700FF>/&gt;</font></html>"
            }
            else{
                uiText = uiText+attributes+"<font color=C700FF>/&gt;</font></html>"
            }
        }
        else{
            uiText = uiText + attributes + "<font color=C700FF>&gt;</font></html>"
        }
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(uiText)
        uinode.add(node)
        uiToXMLHash.put(domnode,node)
        //domnode.metaClass.uiNode = node
        for(int i=0;i<domnode.getChildNodes().length;i++){
            addNodes(domnode.getChildNodes().item(i),node)
            if(i+1==domnode.getChildNodes().length){
                if(domnode.firstChild.class == org.apache.xerces.dom.TextImpl){
                    if(domnode.getChildNodes().length > 1){
                        node.add(new DefaultMutableTreeNode("<html><font color=C700FF>&lt;/${domnode.getTagName()}&gt;</font><html>"))
                        //node.add(new DefaultMutableTreeNode("</${domnode.name}>"))
                    }
                }
                else if(domnode.getChildNodes().length > 0){
                    //node.add(new DefaultMutableTreeNode("</${domnode.name}>"))
                    node.add(new DefaultMutableTreeNode("<html><font color=C700FF>&lt;/${domnode.getTagName()}&gt;</font><html>"))
                }
            }
        }
    }

    class browserTypeList extends JComboBox implements ActionListener  {

        public browserTypeList() {
            String[] browserStrings = ["Internet Explorer", "Chrome", "Firefox"]
            browserStrings.each {
                this.addItem(it)
            }
            addActionListener(this)
        }

        public void actionPerformed(ActionEvent e) {
            SelectedBrowser = this.selectedItem
        }
    }

    public void selectUINodeByXpath(String id){
        def node = null
        try{
            node = xpath.evaluate( id, parser.getDocument(), XPathConstants.NODE )
        }
        catch(Exception ex){
            println "Problem with id: ${id}"
        }
        if(node == null){
            parseHTML(glass.getHTML())
            return
        }
        TreeNode[] nodes = ((DefaultTreeModel) DOMtree.getModel()).getPathToRoot(uiToXMLHash.get(node));
        //TreeNode[] nodes = ((DefaultTreeModel) DOMtree.getModel()).getPathToRoot(node.uiNode);
        TreePath tpath = new TreePath(nodes);
        DOMtree.clearSelection()
        DOMtree.expandPath(tpath)
        DOMtree.scrollPathToVisible(tpath)
        DOMtree.addSelectionPath(tpath)
    }

    public String generateXPathFromDOM(def domnode){
        if (domnode.class == org.apache.xerces.dom.TextImpl) return ""
        if (domnode.class == org.apache.xerces.dom.CommentImpl) return ""
        if (domnode.class == org.apache.html.dom.HTMLDocumentImpl) return ""
        if (domnode.getAttributes() != null){
            def foundID = domnode.getAttributes().nodes.find{it.name == "id"}
            if (foundID != null){
                return "//*[@id='"+foundID.value+"']"
            }
        }

        if (domnode.getTagName() == "HTML"){
            return "/HTML"
        }
        if (domnode.getTagName() == "BODY"){
            return "/HTML/BODY"
        }
        if (domnode.getTagName() == "HEAD"){
            return "/HTML/HEAD"
        }

        int ix= 0
        def siblings = domnode.getParentNode().getChildNodes()
        for(int i=0;i<siblings.length;i++){
            def sibling= siblings.item(i)
            if (sibling==domnode)
                return generateXPathFromDOM(domnode.getParentNode())+'/'+domnode.getTagName()+'['+(ix+1)+']'
            if ((sibling.class != org.apache.xerces.dom.TextImpl && sibling.class != org.apache.xerces.dom.CommentImpl)&& sibling.getTagName()==domnode.getTagName())
                ix++;
        }

    }

    public void parseHTML(String html){
        uiToXMLHash = [:]
        alreadyIncludedHash = [:]
        parser.reset()
        parser.parse(new InputSource(new StringReader(html)))

        /*
        def path = new TreePath(DOMtree.getModel().getRoot())
        Object lastNode = path.getLastPathComponent();
        for (int i = 0; i < DOMtree.getModel().getChildCount(lastNode); i++) {
            Object child = DOMtree.getModel().getChild(lastNode,i);
            TreePath pathToChild = path.pathByAddingChild(child);
            setTreeState(tree,pathToChild,expanded);
        }
        */

        def htmlNode = parser.getDocument().getChildNodes().item(0)
        DOMtree.setModel(null)
        rootNode = new DefaultMutableTreeNode(" <HTML>")
        DefaultTreeModel treeModel = new DefaultTreeModel(rootNode);
        DOMtree.setModel(treeModel)
        addNodes(htmlNode.getChildNodes().item(0),DOMtree.getModel().getRoot())
        addNodes(htmlNode.getChildNodes().item(1),DOMtree.getModel().getRoot())
    }



    public static void main(String[] args){

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                MainWindow main = new MainWindow();
                main.setVisible(true);
            }
        });
    }
}