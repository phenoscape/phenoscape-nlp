package owlaccessor;
//
import java.io.File;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
//TODO replace HermiT with ELK
//import gnu.getopt.LongOpt;
//import gnu.getopt.Getopt;
//import org.semanticweb.HermiT.Reasoner;

/* 
 * Copyright (C) 2007, University of Manchester
 *
 * Modifications to the initial code base are copyright of their
 * respective authors, or their employers as appropriate.  Authorship
 * of the modifications may be determined from the ChangeLog placed at
 * the end of this file.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.

 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/**
 * <p>Simple example. Read an ontology, and display the class hierarchy. May use a
 * reasoner to calculate the hierarchy.</p>
 * 
 * Author: Sean Bechhofer<br>
 * The University Of Manchester<br>
 * Information Management Group<br>
 * Date: 17-03-2007<br>
 * <br>
 */

/*public class Distance {
	
	OWLClass node;
	int distance=0;
	Distance d;
}*/
public class SimpleHierarchyExample {
    private static int INDENT = 4;

    private OWLReasonerFactory reasonerFactory;

    private OWLOntology ontology;

    private PrintStream out;
    Hashtable<OWLClass,Integer> distance = new Hashtable<OWLClass,Integer>();

    private static OWLOntologyManager manager;
    public SimpleHierarchyExample(OWLOntologyManager manager, OWLReasonerFactory reasonerFactory)
            throws OWLException, MalformedURLException {
        this.reasonerFactory = reasonerFactory;
        out = System.out;
    }

    /**
     * Print the class hierarchy for the given ontology from this class down, assuming this class is at
     * the given level. Makes no attempt to deal sensibly with multiple
     * inheritance.
     */
    public void printHierarchy(OWLOntology ontology, OWLClass clazz) throws OWLException {
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(ontology);
        
        this.ontology = ontology;
        printHierarchy(reasoner, clazz, 0 );
        /* Now print out any unsatisfiable classes */
        for (OWLClass cl: ontology.getClassesInSignature()) {
            if (!reasoner.isSatisfiable(cl)) {
                out.println("XXX: " + labelFor(cl));
            }
        }
        reasoner.dispose();
    }
    
    private String labelFor( OWLClass clazz) {
        /*
         * Use a visitor to extract label annotations
         */
        
        LabelExtractor le = new LabelExtractor();
        Set<OWLAnnotation> annotations = clazz.getAnnotations(ontology);
        for (OWLAnnotation anno : annotations) {
            anno.accept(le);
        }
        /* Print out the label if there is one. If not, just use the class URI */
        if (le.getResult() != null) {
            return le.getResult().toString();
        } else {            
            return clazz.getIRI().toString();
        }
    }
    
    /**
     * Print the class hierarchy from this class down, assuming this class is at
     * the given level. Makes no attempt to deal sensibly with multiple
     * inheritance.
     */
    public void printHierarchy(OWLReasoner reasoner, OWLClass clazz, int level)
            throws OWLException {
        TreeSet<OWLClass> list2 = new TreeSet<OWLClass>();
        Set<OWLClass> list1;
        int flag=1;
       // OWLClass nothing = manager.getOWLDataFactory().getOWLClass(OWLRDFVocabulary.OWL_THING.getIRI());

		/*
         * Only print satisfiable classes -- otherwise we end up with bottom
         * everywhere
         */
        if (reasoner.isSatisfiable(clazz)) {
            for (int i = 0; i < level * INDENT; i++) {
                out.print(" ");
            }
            out.println(labelFor( clazz ));
            System.out.println("I am here");
            list1 = reasoner.getSubClasses(clazz, true).getFlattened();
           System.out.println(list1.size());
            /* Find the children and recurse */
          while(flag==1) 
          {
          
           // System.out.println(list1.size());
                for (OWLClass child : list1) {
                    if (!child.isBottomEntity()) {
                    	System.out.println(labelFor( child ));
                    	list2.addAll(reasoner.getSubClasses(child, true).getFlattened());
                    	distance.put(child, level);
                    }
                    else
                    {
                    	distance.put(child, level);
                    }
                }
                list1.clear();
                if(list2.size()!=0)
                	{
                	list1.addAll(list2);
                	list2.clear();
                	level +=1;
                	flag=1;
                	}
                else
                	flag=0;
        }
          System.out.println(distance.size());
    }
    }

    public static void main(String[] args) {
/*        try {

             Handle command line arguments 
            LongOpt[] longopts = new LongOpt[11];
            
            longopts[0] = new LongOpt("help", LongOpt.NO_ARGUMENT, null, '?');
            longopts[1] = new LongOpt("reasoner", LongOpt.REQUIRED_ARGUMENT,
                    null, 'r');
            longopts[2] = new LongOpt("class", LongOpt.REQUIRED_ARGUMENT,
                    null, 'c');

            Getopt g = new Getopt("", args, "?:r:c", longopts);
            int c;
            // String arg;

            IRI classIRI = null;
            String reasonerFactoryClassName = null;
            while ((c = g.getopt()) != -1) {
                switch (c) {
                case '?':
                    System.out.println("command --reasonerFactoryClassName [--class=URL] URL");
                    System.exit(0);
                case 'r':
                     Use a reasoner 
                    reasonerFactoryClassName = g.getOptarg();
                    break;
                case 'c':
                     Class to start from 
                    classIRI = IRI.create(g.getOptarg());
                    break;
                }
            }

            int i = g.getOptind();

            // We first need to obtain a copy of an
            // OWLOntologyManager, which, as the name
            // suggests, manages a set of ontologies. 
            manager = OWLManager.createOWLOntologyManager();
           
            
            // We load an ontology from the URI specified
            // on the command line
            if (args.length <= i) {
                System.out.println("No URI specified!");
                //System.exit(0);
            }

           // System.out.println(args[i]);
            File file = new File("C:/Users/updates/CharaParserTest/Ontologies/ext.owl");
            //IRI documentIRI = IRI.create(args[i]);
            // Now load the ontology.
            OWLOntology ontology = manager.loadOntologyFromOntologyDocument(file);
            // Report information about the ontology
            System.out.println("Ontology Loaded...");
            System.out.println("File: " + file);
            System.out.println("Ontology : " + ontology.getOntologyID());
            System.out.println("Format      : "
                    + manager.getOntologyFormat(ontology));
            // / Create a new SimpleHierarchy object with the given reasoner.
            OWLReasonerFactory reasonerFactoryin = new Reasoner.ReasonerFactory();
            
            
            SimpleHierarchyExample simpleHierarchy = new SimpleHierarchyExample(
                    manager, (OWLReasonerFactory) Class.forName(reasonerFactoryClassName).newInstance());
             
            SimpleHierarchyExample simpleHierarchy = new SimpleHierarchyExample(
                    manager, reasonerFactoryin);
 
            
           
	        // Get Thing
            if (classIRI==null) {
                classIRI = OWLRDFVocabulary.OWL_THING.getIRI();
            	//classIRI.create("http://purl.obolibrary.org/obo/BSPO_0000085");
                
            }
           
            
            OWLClass clazz = manager.getOWLDataFactory().getOWLClass(classIRI);
           // d1.node = clazz;
            
            System.out.println("Class       : " + classIRI);

            // Print the hierarchy below thing
            simpleHierarchy.printHierarchy(ontology, clazz );

        } catch (Exception e) {
            LOGGER.error("", e);
        }*/
    }
}