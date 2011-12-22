package org.exelixis_lab;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Timer;

import javax.swing.JFrame;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;


import ml.LN;
import ml.TreeParser;
import ml.UnorderedPair;

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

};

public class MainFrame extends JFrame {

    JSplitPane splitPane1;
    JSplitPane splitPane2;

    MainFrame() {
	splitPane2 = new JSplitPane();

	splitPane1 = new JSplitPane();

	splitPane1.setRightComponent(splitPane2);

	getContentPane().add( splitPane1 );

	//MainPanel fmp = new MainPanel(configuration, parent)

	HashSet<UnorderedPair<LN, LN>> hs = new HashSet<UnorderedPair<LN,LN>>();
	HashSet<UnorderedPair<LN, LN>> hss[] = new HashSet[1];
	hss[0] = hs;
	
	final ConvertToForester ctf = new ConvertToForester(hss);
	
	
	LN n = TreeParser.parse(new File( "/space/raxml/VINCENT/RAxML_bipartitions.150.BEST.WITH" ));
	
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

	org.forester.archaeopteryx.MainFrameApplication x = (MainFrameApplication) org.forester.archaeopteryx.Archaeopteryx.createApplication(phys, tmp.getPath(), "" );
	//x.remove(x.getMainPanel());
	final org.forester.archaeopteryx.MainPanel mp = x.getMainPanel();
	//JTabbedPane tp = mp.hijackTabbedPane();
	
	
	
	splitPane1.setLeftComponent(x.getMainPanel());
	
	x.setVisible(false);

	
	Thread t = new Thread( new Runnable() {
	    
	    boolean isOn = false;
	    
	    @Override
	    public void run() {
		// TODO Auto-generated method stub
		Random rnd = new Random();
		
		while( true ) { 
        	
		    for( int i = 0; i < 10; ++i ) {
			int idx = rnd.nextInt(ctf.branchData.size() );
			ctf.branchData.get(idx).setBranchColor(new BranchColor(new Color( rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255) )));
		    }
		     
		    
		    mp.repaint();
        	
		    try {
			Thread.sleep(100);
		    } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		    }
		}
		
	    }
	});
	
	t.start();
	
    }


    public static void main(String[] args) {
	MainFrame mf = new MainFrame();
	mf.setVisible(true);
    }
}


