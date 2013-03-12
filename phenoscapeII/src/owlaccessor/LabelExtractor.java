package owlaccessor;
/*
2   * This file is part of the OWL API.
3   *
4   * The contents of this file are subject to the LGPL License, Version 3.0.
5   *
6   * Copyright (C) 2011, The University of Manchester
7   *
8   * This program is free software: you can redistribute it and/or modify
9   * it under the terms of the GNU General Public License as published by
10  * the Free Software Foundation, either version 3 of the License, or
11  * (at your option) any later version.
12  *
13  * This program is distributed in the hope that it will be useful,
14  * but WITHOUT ANY WARRANTY; without even the implied warranty of
15  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
16  * GNU General Public License for more details.
17  *
18  * You should have received a copy of the GNU General Public License
19  * along with this program.  If not, see http://www.gnu.org/licenses/.
20  *
21  *
22  * Alternatively, the contents of this file may be used under the terms of the Apache License, Version 2.0
23  * in which case, the provisions of the Apache License Version 2.0 are applicable instead of those above.
24  *
25  * Copyright 2011, University of Manchester
26  *
27  * Licensed under the Apache License, Version 2.0 (the "License");
28  * you may not use this file except in compliance with the License.
29  * You may obtain a copy of the License at
30  *
31  * http://www.apache.org/licenses/LICENSE-2.0
32  *
33  * Unless required by applicable law or agreed to in writing, software
34  * distributed under the License is distributed on an "AS IS" BASIS,
35  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
36  * See the License for the specific language governing permissions and
37  * limitations under the License.
38  */ 
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationObjectVisitor;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;

/*Simple visitor that grabs any labels on an entity.
Author: Sean Bechhofer
The University Of Manchester
Information Management Group
Date: 17-03-2007*/


public class LabelExtractor implements OWLAnnotationObjectVisitor {
    String result;
 
    public LabelExtractor() {
         result = null;
     }
 
    public void visit(OWLAnonymousIndividual individual) {}
 
    public void visit(IRI iri) {}
 
     public void visit(OWLLiteral literal) {}
 
    public void visit(OWLAnnotation annotation) {
         /*
          * If it's a label, grab it as the result. Note that if there are
78          * multiple labels, the last one will be used.
79          */
        if (annotation.getProperty().isLabel()) {
             OWLLiteral c = (OWLLiteral) annotation.getValue();
             result = c.getLiteral();
         }
     }
 
     public void visit(OWLAnnotationAssertionAxiom axiom) {}
 
     public void visit(OWLAnnotationPropertyDomainAxiom axiom) {}
 
     public void visit(OWLAnnotationPropertyRangeAxiom axiom) {}
 
     public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {}

     public void visit(OWLAnnotationProperty property) {}
 
     public void visit(OWLAnnotationValue value) {}
 
     public String getResult() {
         return result;
    }
}

 

 

