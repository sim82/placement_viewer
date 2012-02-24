package org.exelixis_lab;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Timer;

import javax.swing.AbstractListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;


import ml.LN;
import ml.TreeParser;
import ml.UnorderedPair;

import org.exelixis_lab.PlacementResultsLHWeight.QS;
import org.exelixis_lab.PlacementResultsLHWeight.QS.Placement;
import org.forester.archaeopteryx.MainFrameApplication;
import org.forester.archaeopteryx.MainPanel;
import org.forester.archaeopteryx.TreePanel;
import org.forester.phylogeny.Phylogeny;
import org.forester.phylogeny.PhylogenyNode;
import org.forester.phylogeny.PhylogenyNodeI;
import org.forester.phylogeny.data.BranchColor;
import org.forester.phylogeny.data.BranchData;

class ConvertToForester {
    
    public ArrayList<BranchData> branchData = new ArrayList<BranchData>();
    Map<String,BranchData> branchDataMap = new HashMap<String,BranchData>();
    
    
    
    final HashSet<UnorderedPair<LN, LN>>[] branchFound;

    ConvertToForester(HashSet<UnorderedPair<LN, LN>>[] branchFound) {
	this.branchFound = branchFound;
    }

    void colorBlend(int[] c1, int[] c2) {
	for (int i = 0; i < c1.length; i++) {
	    c1[i] += c2[i];
	    c1[i] = Math.min(c1[i], 255);
	}

    }
    int nfound = 0;
    PhylogenyNode trav(LN n, boolean back) {
	PhylogenyNode fn = new PhylogenyNode();

	if (!back) {

	    final int[] color;
	    
	    final int[] x = { 255, 255, 255 }; // java initializer lists
						   // syntax is stupid!
	    color = x;
		
	    BranchData bd = new BranchData();
	    
	    bd.setBranchColor(new BranchColor(new Color(color[0], color[1],
		    color[2])));

	    
	    
	    if (n.data.isTip) {
		fn.setName(n.data.getTipName());
	    }

	    fn.setBranchData(bd);
	    fn.setDistanceToParent(n.backLen);

	    branchData.add( bd );
	    String name = n.backLabel;
	    
	    System.out.printf( "name: %s\n", name );
	    branchDataMap.put(name, bd);
	    
	    
	}

	if (back) {
	    assert (n.back != null);
	    fn.addAsChild(trav(n.back, false));
	}
	if (!n.data.isTip) {

	    fn.addAsChild(trav(n.next.back, false));
	    fn.addAsChild(trav(n.next.next.back, false));
	}
	return fn;
    }

    
    BranchData getBranchData( String name ) {
	return branchDataMap.get(name);
    }
};


class PlacementResultsLHWeight extends AbstractListModel {
    static class QS extends AbstractTableModel implements Comparable<QS> {
	String name;
	
	class Placement {
	    String branchName;
	    float lhWeight;
	    
	    Placement( String branchName, float lhw ) {
		this.branchName = branchName;
		this.lhWeight = lhw;
	    }
	}
	
	ArrayList<Placement> placements = new ArrayList<PlacementResultsLHWeight.QS.Placement>();
	
	
	QS( String name ) {
	    this.name = name;
	}
	
	void add( String branchName, float lhw ) {
	    placements.add( new Placement(branchName, lhw));
//	    fireContentsChanged(this, placements.size() - 1, placements.size() - 1 );
	    fireTableRowsInserted( placements.size() - 1, placements.size() - 1 );
	    
	}

//	@Override
//	public int getSize() {
//	    return placements.size();
//	}
//
//	@Override
//	public Object getElementAt(int index) {
//	    final Placement p = placements.get(index);
//	    
//	    String ret = p.branchName;
//	    while( ret.length() < 4 ) {
//		ret = ret + " ";
//	    }
//	    return ret + " " + p.lhWeight;
//	}
	
	
	String getName() {
	    return name;
	}
	
	Placement getPlacement( int index ) {
	    return placements.get(index);
	}

	@Override
	public int getRowCount() {
	    return placements.size();
	}

	@Override
	public int getColumnCount() {
	    return 2;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
	    final Placement p = placements.get(rowIndex);
	    
	    if( columnIndex == 0 ) {
		return p.branchName;
	    } else {
		return "" + p.lhWeight;
	    }
	    
	    
	}

	@Override
	public int compareTo(QS o) {
	    return name.compareTo(o.name);
	}
	
    }
    
    
    
    Map<String,QS> qsMap = new HashMap<String, PlacementResultsLHWeight.QS>();
    ArrayList<QS> qsList = new ArrayList<PlacementResultsLHWeight.QS>();
    
    PlacementResultsLHWeight( BufferedReader r ) {
	String line;
	
	try {
	    
	    while( (line = r.readLine()) != null ) {
	        StringTokenizer st = new StringTokenizer(line);
	        if( st.countTokens() != 4 ) {
	            throw new RuntimeException( "bad line in classification file (not 4 token): '" + line + "'" );
	        }
	        String qsName = st.nextToken();
	            
	        QS qs = qsMap.get(qsName);
	        if( qs == null ) {
	            qs = new QS( qsName );
	            qsMap.put( qsName, qs );
	            qsList.add( qs );
	            fireIntervalAdded(this, qsList.size() - 1, qsList.size() - 1);
	        }
	        
	        String branchName = st.nextToken();
	        float lhw = Float.parseFloat(st.nextToken());
	        
	        qs.add( branchName, lhw );
	        Collections.sort(qsList);
		
	    }
	    
	    
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	
	
    }

    @Override
    public int getSize() {
	return qsList.size();
    }

    @Override
    public Object getElementAt(int index) {
	return qsList.get( index ).getName();
    }
    
    
    public QS getQS( int index ) {
	return qsList.get(index);
    }
    
    
}

class TestPane extends JPanel {
    JLabel label = new JLabel();
    JButton button = new JButton();
    
    File fileName;
    
    String fileFilterPrefix;
    
    //boolean fileValid = false;
    
    void setFileName( File name) {
	fileName = name;
	label.setText(fileName.getName());
	
	
    }
    
    File getFile() {
	boolean fileValid = fileName != null && fileName.isFile() && fileName.canRead();
	if( fileValid ) {
	    return fileName;
	} else {
	    return null;
	}
    }
    
    TestPane( String initText, String prefix  ) {
	super();
	
	fileFilterPrefix = prefix;
	
	label.setText(initText);
	button.setText("...");
	
	final TestPane parent = this; 
	
	button.addActionListener(new ActionListener() {
	    
	    @Override
	    public void actionPerformed(ActionEvent e) {
		JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
		// Note: source for ExampleFileFilter can be found in FileChooserDemo,
		// under the demo/jfc directory in the JDK.
		
		
		FileFilter f = new FileFilter() {
		    
		    @Override
		    public String getDescription() {
			return parent.fileFilterPrefix;
			//return null;
		    }
		    
		    @Override
		    public boolean accept(File f) {
			// TODO Auto-generated method stub
			//return false;
			
			return f.isDirectory() || f.getName().startsWith(parent.fileFilterPrefix);
		    }
		};
		chooser.setFileFilter(f);
		
		
		
		int returnVal = chooser.showOpenDialog(parent);
		if(returnVal == JFileChooser.APPROVE_OPTION) {
		    System.out.println("You chose to open this file: " +
			    chooser.getSelectedFile().getName());
		    
		    parent.setFileName( chooser.getSelectedFile() );
		}		
		 
		
	    }
	});
	
	add(label);
	
	add(button);
	
	
	
    }
}

public class PlacementViewer extends JFrame {

    JSplitPane splitPane1;
    JSplitPane splitPane2;

    
    static File inferOtherFile( File name, String oldPrefix, String newPrefix ) {
	
	String parent = name.getParent();
	String file = name.getName();
	
	file = file.replace( oldPrefix, newPrefix );
	
	File newName = new File( parent, file );
	
	if( newName.isFile() && name.canRead()) {
	    return newName;
	} else {
	    return null;
	}
	
    }
        
    PlacementViewer() {
	
	TestPane treeChoser = new TestPane( "Select tree file", "RAxML_originalLabelledTree." );
	TestPane placementChoser = new TestPane( "Select placement file", "RAxML_classificationLikelihoodWeights" );
	
	Object complexMsg[] = { "Please select the RAxML_originalLabelledTree and RAxML_classificationLikelihoodWeights files to visualize.\n\nIf you chose only one file, the name of the other file will be determined automatically, if possible.", treeChoser, placementChoser };

//	JOptionPane optionPane = new JOptionPane();
//	optionPane.setMessage(complexMsg);
//	optionPane.setMessageType(JOptionPane.INFORMATION_MESSAGE);
//	JDialog dialog = optionPane.createDialog(null, "Width 100");
//	dialog.setVisible(true);
	
	JOptionPane.showMessageDialog( null, complexMsg);
	
	File treeFile = treeChoser.getFile();
	File placementFile = placementChoser.getFile();
	
	if( treeFile == null ) {
	    
	    if( placementFile != null ) {
		treeFile = inferOtherFile(placementFile, "RAxML_classificationLikelihoodWeights.", "RAxML_originalLabelledTree.");
	    }
	    
	    if( treeFile == null ) {
		JOptionPane.showMessageDialog(null, "The selected tree-file is not readable" );
		throw new RuntimeException("bailing out");
	    }
	}
	if( placementFile == null ) {
	    if( treeFile != null ) {
		placementFile = inferOtherFile( treeFile, "RAxML_originalLabelledTree.", "RAxML_classificationLikelihoodWeights." );
	    }
	    
	    if( placementFile == null ) {
		JOptionPane.showMessageDialog(null, "The selected placement-file is not readable" );
		throw new RuntimeException("bailing out");
	    }
	}
	
	
	splitPane2 = new JSplitPane();
	splitPane1 = new JSplitPane();

	splitPane1.setRightComponent(splitPane2);

	getContentPane().add( splitPane1 );

	setSize(new Dimension(1024, 768));
	splitPane1.setResizeWeight(0.7);
	splitPane2.setResizeWeight(0.7);
	
	
	//MainPanel fmp = new MainPanel(configuration, parent)

	HashSet<UnorderedPair<LN, LN>> hs = new HashSet<UnorderedPair<LN,LN>>();
	HashSet<UnorderedPair<LN, LN>> hss[] = new HashSet[1];
	hss[0] = hs;
	
	final ConvertToForester ctf = new ConvertToForester(hss);
	
	//RAxML_classificationLikelihoodWeights.707700542
	LN n = TreeParser.parse( treeFile );
	
	PhylogenyNode pn = ctf.trav(n, true);
	Phylogeny phy = new Phylogeny();
	phy.setRoot(pn);
	
	
	Phylogeny[] phys = {phy};

	final File tmp;	    
	try {
	    tmp = File.createTempFile("arc", ".txt");
	    ClassLoader cl = getClass().getClassLoader();

	    InputStream is = cl.getResourceAsStream("_aptx_configuration_file.txt");
	    FileOutputStream os = new FileOutputStream(tmp);
	    byte[] buf = new byte[4096];
	    while(true ) {
		int read = is.read(buf);
		os.write(buf,0,read);

		if( read != buf.length ) {
		    break;
		}
	    }
	    os.close();
	    tmp.deleteOnExit();
	} catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    throw new RuntimeException( "error while creating temp file" );
	}

	org.forester.archaeopteryx.MainFrameApplication mainFrame = (MainFrameApplication) org.forester.archaeopteryx.Archaeopteryx.createApplication(phys, tmp.getPath(), "" );
	//x.remove(x.getMainPanel());
	final org.forester.archaeopteryx.MainPanel mp = mainFrame.getMainPanel();
	//JTabbedPane tp = mp.hijackTabbedPane();
	
	
	
	splitPane1.setLeftComponent(mainFrame.getMainPanel());
	
	final PlacementResultsLHWeight placementResults;
	try {
	    placementResults = new PlacementResultsLHWeight( new BufferedReader(new FileReader(placementFile)));
	} catch (FileNotFoundException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	    throw new RuntimeException( "bailing out" );
	}
	
	
	final JList qsList = new JList( placementResults );
	
	qsList.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
	
	JScrollPane qsScrollPane = new JScrollPane(qsList);
	qsScrollPane.setMinimumSize(new Dimension( 200, 200 ));
	
	splitPane2.setLeftComponent( new JScrollPane(qsList));
	
	//final JList placementList = new JList();
	final JTable placementTable = new JTable();
	placementTable.setColumnSelectionAllowed(false);
	splitPane2.setRightComponent( new JScrollPane(placementTable));
	
	qsList.addListSelectionListener(new ListSelectionListener() {
	    
	    @Override
	    public void valueChanged(ListSelectionEvent e) {
		int index = qsList.getSelectedIndex();
		
		PlacementResultsLHWeight.QS qs = placementResults.getQS(index);
//		placementList.setModel(qs);
//		placementList.getSelectionModel().setSelectionInterval(0, qs.getSize() - 1);
		placementTable.setModel(qs);
		placementTable.getSelectionModel().setSelectionInterval(0, qs.getRowCount() - 1);
		
	    }
	});
	
	
	
	placementTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
	    

	    int clampColor( float c ) {
		return Math.max( 0, Math.min( (int)c, 255 ));
	    }
	    Color interpolate( Color c1, Color c2, float v ) {
		int dr = c2.getRed() - c1.getRed();
		int dg = c2.getGreen() - c1.getGreen();
		int db = c2.getBlue() - c1.getBlue();
		
		
		int r = clampColor(c1.getRed() + v * dr);
		int g = clampColor(c1.getGreen() + v * dg);
		int b = clampColor(c1.getBlue() + v * db);
		//System.err.printf( "%d %d %d\n", r, g, b );
		return new Color( r, g, b );
	    }
	    
	    @Override
	    public void valueChanged(ListSelectionEvent e) {
		// TODO Auto-generated method stub
		int[] selection = placementTable.getSelectedRows();
		
		PlacementResultsLHWeight.QS qs = (QS) placementTable.getModel();
		
		for( BranchData b : ctf.branchData ) {
		    b.setBranchColor( new BranchColor(Color.WHITE));
		}
//		float weightSum = 0;
//		
//		for( int s : selection ) {
//		    Placement p = qs.getPlacement( s );
//		    weightSum += p.lhWeight;
//		    
//		}
		for( int s : selection ) {
		    Placement p = qs.getPlacement( s );
		    
		    //ctf.branchDataMap.get(p.branchName).setBranchColor(new BranchColor( Color.RED ));
		    Color col = interpolate(Color.RED, Color.GREEN, p.lhWeight);
		    
		    ctf.branchDataMap.get(p.branchName).setBranchColor(new BranchColor( col ));
		    
		}
		
		 mp.repaint();
	    }
	});
	
	
	
	
	//splitPane2.setLeftComponent(new JList(placementResults.get));
	setDefaultCloseOperation( WindowConstants.EXIT_ON_CLOSE );
	mainFrame.validate();
	mainFrame.setVisible(false);

//	
//	Thread t = new Thread( new Runnable() {
//	    
//	    boolean isOn = false;
//	    
//	    @Override
//	    public void run() {
//		// TODO Auto-generated method stub
//		Random rnd = new Random();
//		
//		while( true ) { 
//        	
//		    for( int i = 0; i < 10; ++i ) {
//			int idx = rnd.nextInt(ctf.branchData.size() );
//			ctf.branchData.get(idx).setBranchColor(new BranchColor(new Color( rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255) )));
//		    }
//		     
//		    
//		    mp.repaint();
//        	
//		    try {
//			Thread.sleep(100);
//		    } catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		    }
//		}
//		
//	    }
//	});
//	
//	t.start();
	
    }


    public static void main(String[] args) {
	PlacementViewer mf = new PlacementViewer();
	mf.setVisible(true);
    }
}


