/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: XmlParam.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.technology.xml;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Poly.Type;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.EdgeH;
import com.sun.electric.technology.EdgeV;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.tool.Job;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 */
public class XmlParam {

    public static class Technology implements Serializable {
        public String techName;
        public String className;
        public String shortTechName;
        public String description;
        public int minNumMetals;
        public int maxNumMetals;
        public int defaultNumMetals;
        public double scaleValue;
        public boolean scaleRelevant;
        public String defaultFoundry;
        public double minResistance;
        public double minCapacitance;
        private final LinkedHashMap<String,Layer> layers = new LinkedHashMap<String,Layer>();
        public final List<ArcProto> arcs = new ArrayList<ArcProto>();
        public final List<PrimitiveNode> nodes = new ArrayList<PrimitiveNode>();
        public final List<SpiceHeader> spiceHeaders = new ArrayList<SpiceHeader>();
        public final List<DisplayStyle> displayStyles = new ArrayList<DisplayStyle>();
        public MenuPalette menuPalette;
        public final LinkedHashMap<String,RuleSet> ruleSets = new LinkedHashMap<String,RuleSet>();
        public final List<Foundry> foundries = new ArrayList<Foundry>();

        public Layer newLayer(String name) {
            if (name == null)
                throw new NullPointerException();
            if (layers.containsKey(name))
                throw new IllegalArgumentException("Duplicate Layer " + name);
            Layer layer = new Layer(name);
            layers.put(name, layer);
            return layer;
        }

        public RuleSet newRuleSet(String name) {
            if (name == null)
                throw new NullPointerException();
            if (ruleSets.containsKey(name))
                throw new IllegalArgumentException("Duplicate RuleSet " + name);
            RuleSet ruleSet = new RuleSet(name);
            ruleSets.put(name, ruleSet);
            return ruleSet;
        }

        public Layer findLayer(String name) {
            return layers.get(name);
        }

        public ArcProto findArc(String name) {
            for (ArcProto arc: arcs) {
                if (arc.name.equals(name))
                    return arc;
            }
            return null;
        }

        public PrimitiveNode findNode(String name) {
            for (PrimitiveNode node: nodes) {
                if (node.name.equals(name))
                    return node;
            }
            return null;
        }

        public void writeXml(String fileName) {
            try {
                PrintWriter out = new PrintWriter(fileName);
                Writer writer = new Writer(out);
                writer.writeTechnology(this);
                out.close();
                System.out.println("Wrote " + fileName);
                System.out.println(" (Add this file to the 'Added Technologies' Project Preferences to install it in Electric)");
            } catch (IOException e) {
                System.out.println("Error creating " + fileName);
            }
        }
    }

    public static class Layer implements Serializable {
        public final String name;
        public com.sun.electric.technology.Layer.Function function;
        public int extraFunction;
        public String cif;
        public String skill;
        public double resistance;
        public double capacitance;
        public double edgeCapacitance;
        public PureLayerNode pureLayerNode;

        private Layer(String name) {
            this.name = name;
        }
    }

    public static class PureLayerNode implements Serializable {
        public String name;
        public String oldName;
        public Type style;
        public String port;
        public final List<String> portArcs = new ArrayList<String>();
    }

    public static class ArcProto implements Serializable {
        public String name;
        public String oldName;
        public com.sun.electric.technology.ArcProto.Function function;
        public boolean wipable;
        public boolean curvable;
        public boolean special;
        public boolean notUsed;
        public boolean skipSizeInPalette;

        public boolean extended;
        public boolean fixedAngle;
        public int angleIncrement;
        public double antennaRatio;
        public double elibWidthOffset;
        public final List<ArcLayer> arcLayers = new ArrayList<ArcLayer>();
        public ArcPin arcPin;
    }

    public static class ArcPin implements Serializable {
        public String name;
        public String portName;
        public double elibSize;
        public final List<String> portArcs = new ArrayList<String>();
    }

    public static class ArcLayer implements Serializable {
        public String layer;
        public final Distance extend = new Distance();
        public Type style;
    }

    public static class PrimitiveNode implements Serializable {
        public String name;
        public String oldName;
        public boolean shrinkArcs;
        public boolean square;
        public boolean canBeZeroSize;
        public boolean wipes;
        public boolean lockable;
        public boolean edgeSelect;
        public boolean skipSizeInPalette;
        public boolean notUsed;
        public boolean lowVt;
        public boolean highVt;
        public boolean nativeBit;
        public boolean od18;
        public boolean od25;
        public boolean od33;

        public com.sun.electric.technology.PrimitiveNode.Function function;
        public EPoint diskOffset;
        public final Distance defaultWidth = new Distance();
        public final Distance defaultHeight = new Distance();
        public ERectangle nodeBase;
        public final List<NodeLayer> nodeLayers = new ArrayList<NodeLayer>();
        public final List<PrimitivePort> ports = new ArrayList<PrimitivePort>();
        public int specialType;
        public double[] specialValues;
        public NodeSizeRule nodeSizeRule;
        public String spiceTemplate;
    }

    public static class NodeLayer implements Serializable {
        public String layer;
        public Type style;
        public int portNum;
        public boolean inLayers;
        public boolean inElectricalLayers;
        public int representation;
        public final Distance lx = new Distance();
        public final Distance hx = new Distance();
        public final Distance ly = new Distance();
        public final Distance hy = new Distance();
        public final List<TechPoint> techPoints = new ArrayList<TechPoint>();
        public double sizex, sizey, sep1d, sep2d;
        public String sizeRule, sepRule, sepRule2D;
        public double lWidth, rWidth, tExtent, bExtent;
    }

    public static class NodeSizeRule implements Serializable {
        public double width;
        public double height;
        public String rule;
    }

    public static class PrimitivePort implements Serializable {
        public String name;
        public int portAngle;
        public int portRange;
        public int portTopology;
        public final Distance lx = new Distance();
        public final Distance hx = new Distance();
        public final Distance ly = new Distance();
        public final Distance hy = new Distance();
        public final List<String> portArcs = new ArrayList<String>();
    }

    public static class SpiceHeader implements Serializable {
        public int level;
        public final List<String> spiceLines = new ArrayList<String>();
    }

    public static class DisplayStyle implements Serializable {
        public String name;
        public final List<Color> transparentLayers = new ArrayList<Color>();
        private final LinkedHashMap<Layer,LayerDisplayStyle> layerStylesInternal = new LinkedHashMap<Layer,LayerDisplayStyle>();
        public final Map<Layer,LayerDisplayStyle> layerStyles = Collections.unmodifiableMap(layerStylesInternal);

        public LayerDisplayStyle newLayer(Layer layer) {
            LayerDisplayStyle lds = new LayerDisplayStyle(layer);
            LayerDisplayStyle old = layerStylesInternal.put(layer, lds);
            assert old == null;
            return lds;
        }
    }

    public static class LayerDisplayStyle implements Serializable {
        public final Layer layer;
        public EGraphics desc;
        public String mode3D;
        public double factor3D;

        private LayerDisplayStyle(Layer layer) {
            this.layer = layer;
        }
    }

    public static class MenuPalette implements Serializable {
        public int numColumns;
        public List<List<Object>> menuBoxes = new ArrayList<List<Object>>();

        public String writeXml() {
            StringWriter sw = new StringWriter();
            PrintWriter out = new PrintWriter(sw);
            OneLineWriter writer = new OneLineWriter(out);
            writer.writeMenuPaletteXml(this);
            out.close();
            return sw.getBuffer().toString();
        }
    }

    public static class MenuNodeInst implements Serializable {
        public String protoName;
        public com.sun.electric.technology.PrimitiveNode.Function function;
        public String text;
        public double fontSize;
        public int rotation;
    }

    public static class MenuCell implements Serializable {
        public String cellName;
//        public String text;
//        public double fontSize;
    }

    public static class Distance implements Serializable {
        public double k;
        public double lambdaValue;
        public final List<DistanceRule> terms = new ArrayList<DistanceRule>();

        public void assign(Distance d) {
            k = d.k;
            lambdaValue = d.lambdaValue;
            for (DistanceRule term: d.terms)
                terms.add(term.clone());
        }

//        public void assign(com.sun.electric.technology.Technology.Distance d) {
//            k = d.k;
//            lambdaValue = d.lambdaValue;
//            for (com.sun.electric.technology.Technology.DistanceRule term: d.terms)
//                terms.add(new DistanceRule(term));
//        }

        public Distance clone() {
            Distance d = new Distance();
            d.assign(this);
            return d;
        }

        public double getLambda(DistanceContext context) {
            double value = lambdaValue;
            for (DistanceRule term: terms)
                value += term.getLambda(context);
            return value;
        }
        private void writeXml(Writer writer, boolean multiLine) {
            for (DistanceRule term: terms) {
                term.writeXml(writer);
                if (multiLine)
                    writer.l();
            }
            if (lambdaValue != 0) {
                writer.bcpe(XmlKeyword.lambda, lambdaValue);
                if (multiLine)
                    writer.l();
            }
        }
        public void addLambda(double value) {
            lambdaValue += value;
        }
        public void addRule(String ruleName, double k) {
            addRule(ruleName, null, k);
        }
        public void addRule(String ruleName, Layer layer, double k) {
            addRule(ruleName, layer, null, k);
        }
        public void addRule(String ruleName, Layer layer, Layer layer2, double k) {
            terms.add(new DistanceRule(ruleName, layer, layer2, k));
        }
        public boolean isEmpty() { return lambdaValue == 0 && terms.isEmpty(); }
    }

    public static interface DistanceContext {
        public double getRule(String ruleName);
    }

    public static class DistanceRule implements Serializable, Cloneable {
        final String ruleName;
        final Layer layer;
        final Layer layer2;
        final double k;

//        public DistanceRule(com.sun.electric.technology.Technology.DistanceRule oldRule) {
//            this(oldRule.ruleName, null, null, oldRule.k);
//        }

        private DistanceRule(String ruleName, Layer layer, Layer layer2, double k) {
            this.ruleName = ruleName;
            this.layer = layer;
            this.layer2 = layer2;
            this.k = k;
        }

        public DistanceRule clone() {
            try {
                return (DistanceRule)super.clone();
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }

        private void writeXml(Writer writer) {
            writer.b(XmlKeyword.rule);
            writer.a("ruleName", ruleName);
            if (layer != null) {
                writer.a("layer", layer.name);
                if (layer2 != null)
                    writer.a("layer2", layer2.name);
            }
            if (k != 1)
                writer.a("k", k);
            writer.e();
        }

        private double getLambda(DistanceContext context) {
            return context.getRule(ruleName)*k;
        }
    }

    public static class RuleSet implements Serializable {
        public final String name;
        public final Map<String,Map<Layer,Distance>> layerRules = new LinkedHashMap<String,Map<Layer,Distance>>();

        private RuleSet(String name) {
            this.name = name;
        }

        public Map<Layer,Distance> newLayerRule(String ruleName) {
            if (ruleName == null)
                throw new NullPointerException();
            if (layerRules.containsKey(ruleName))
                throw new IllegalArgumentException("Duplicate LayerRule " + ruleName);
            Map<Layer,Distance> layerRule = new LinkedHashMap<Layer,Distance>();
            layerRules.put(ruleName, layerRule);
            return layerRule;
        }
    }

    public static class Foundry implements Serializable {
        public String name;
        public final Map<String,String> layerGds = new LinkedHashMap<String,String>();
        public final List<DRCTemplate> rules = new ArrayList<DRCTemplate>();
    }

    private XmlParam() {}

    private static enum XmlKeyword {
        technology,
        shortName(true),
        description(true),
        numMetals,
        scale,
        defaultFoundry,
        minResistance,
        minCapacitance,
        layer,
        display3D,
        cifLayer,
        skillLayer,
        parasitics,
        pureLayerNode,

        arcProto,
        oldName(true),
        wipable,
        curvable,
        special,
        notUsed,
        skipSizeInPalette,

        extended(true),
        fixedAngle(true),
        angleIncrement(true),
        antennaRatio(true),
        elibWidthOffset(true),

        arcLayer,
        arcPin,

        primitiveNode,
        //oldName(true),
        shrinkArcs,
        square,
        canBeZeroSize,
        wipes,
        lockable,
        edgeSelect,
//        skipSizeInPalette,
//        notUsed,
        lowVt,
        highVt,
        nativeBit,
        od18,
        od25,
        od33,
        diskOffset,
        defaultWidth,
        defaultHeight,
        nodeBase,
        nodeLayer,
        box,
        multicutbox,
        serpbox,
        lambdaBox,
        points,
        techPoint,
        primitivePort,
        portAngle,
        portTopology(true),
//        techPoint,
        portArc(true),
        polygonal,
        serpTrans,
        specialValue(true),
        minSizeRule,
        spiceTemplate,
        spiceHeader,
        spiceLine,

        displayStyle,
        transparentLayer,
        r(true),
        g(true),
        b(true),
        transparentColor,
        opaqueColor,
        patternedOnDisplay(true),
        patternedOnPrinter(true),
        pattern(true),
        outlined(true),
        opacity(true),
        foreground(true),

        menuPalette,
        menuBox,
        menuArc(true),
        menuNode(true),
        menuCell,
        menuText(true),
        menuNodeInst,
        menuNodeText,
        lambda(true),
        rule,

        ruleSet,
        layerRule,
        Foundry,
        layerGds,
        LayerRule,
        LayersRule,
        NodeLayersRule,
        NodeRule;

        private final boolean hasText;

        private XmlKeyword() {
            hasText = false;
        };
        private XmlKeyword(boolean hasText) {
            this.hasText = hasText;
        }
    };

    private static final HashMap<String,XmlKeyword> xmlKeywords = new HashMap<String,XmlKeyword>();
    static {
        for (XmlKeyword k: XmlKeyword.class.getEnumConstants())
            xmlKeywords.put(k.name(), k);
    }

    private static Schema schema = null;

    private static synchronized void loadTechnologySchema() throws SAXException {
        if (schema != null) return;
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        URL technologySchemaUrl = Technology.class.getResource("xml/TechnologyParam.xsd");
        if (technologySchemaUrl != null)
            schema = schemaFactory.newSchema(technologySchemaUrl);
        else
        {
            System.err.println("Schema file TechnologyParam.xsd, working without XML schema");
            System.out.println("Schema file TechnologyParam.xsd, working without XML schema");
        }
    }

    public static Technology parseTechnology(URL fileURL) {
//        System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            if (schema == null)
                loadTechnologySchema();
            factory.setSchema(schema);
    //        factory.setValidating(true);
    //        System.out.println("Memory usage " + Main.getMemoryUsage() + " bytes");
            // create the parser
            long startTime = System.currentTimeMillis();
            SAXParser parser = factory.newSAXParser();
            URLConnection urlCon = fileURL.openConnection();
            InputStream inputStream = urlCon.getInputStream();

            XMLReader handler = new XMLReader();
            parser.parse(inputStream, handler);
            if (Job.getDebug())
            {
                long stopTime = System.currentTimeMillis();
                System.out.println("Loading technology " + fileURL + " ... " + (stopTime - startTime) + " msec");
            }
            return handler.tech;
        } catch (SAXParseException e) {
            String msg = "Error parsing Xml technology:\n" +
                    e.getMessage() + "\n" +
                    " Line " + e.getLineNumber() + " column " + e.getColumnNumber() + " of " + fileURL;
            msg = msg.replaceAll("\"http://electric.sun.com/Technology\":", "");
            System.out.println(msg);
            Job.getUserInterface().showErrorMessage(msg, "Error parsing Xml technology");
        } catch (Exception e) {
            String msg = "Error loading Xml technology " + fileURL + " :\n"
                   + e.getMessage() + "\n";
            System.out.println(msg);
            Job.getUserInterface().showErrorMessage(msg, "Error loading Xml technology");
        }
        return null;
    }

    /**
     * Method to parse a string of XML that describes the component menu in a Technology Editing context.
     * Normal parsing of XML returns objects in the Xml class, but
     * this method returns objects in a given Technology-Editor world.
     * @param xml the XML string
     * @param nodes the PrimitiveNode objects describing nodes in the technology.
     * @param arcs the ArcProto objects describing arcs in the technology.
     * @return the MenuPalette describing the component menu.
     */
    public static MenuPalette parseComponentMenuXMLTechEdit(String xml, List<PrimitiveNode> nodes, List<ArcProto> arcs)
    {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        factory.setNamespaceAware(true);
        try
        {
            SAXParser parser = factory.newSAXParser();
            InputSource is = new InputSource(new StringReader(xml));
            XMLReader handler = new XMLReader(nodes, arcs);
            parser.parse(is, handler);
            return handler.tech.menuPalette;
        } catch (Exception e)
        {
            System.out.println("Error parsing XML component menu data");
            e.printStackTrace();
        }
        return null;
    }

    private static class XMLReader extends DefaultHandler {
        private static boolean DEBUG = false;
        private Locator locator;

        private Technology tech = new Technology();
        private Layer curLayer;
        private ArcProto curArc;
        private PrimitiveNode curNode;
        private NodeLayer curNodeLayer;
        private PrimitivePort curPort;
        private int curSpecialValueIndex;
        private ArrayList<Object> curMenuBox;
        private MenuNodeInst curMenuNodeInst;
        private MenuCell curMenuCell;
        private Distance curDistance;
        private SpiceHeader curSpiceHeader;

        private DisplayStyle curDisplayStyle;
        private int curTransparent = 0;
        private int curR;
        private int curG;
        private int curB;
        private LayerDisplayStyle curLayerDisplayStyle;
        private boolean patternedOnDisplay;
        private boolean patternedOnPrinter;
        private final int[] pattern = new int[16];
        private int curPatternIndex;
        private EGraphics.Outline outline;
        private double opacity;
        private boolean foreground;

        private RuleSet curRuleSet;
        private Map<Layer,Distance> curLayerRule;
        private Foundry curFoundry;

        private boolean acceptCharacters;
        private StringBuilder charBuffer = new StringBuilder();
        private Attributes attributes;

        XMLReader() {
        }

        XMLReader(List<PrimitiveNode> nodes, List<ArcProto> arcs)
        {
        	for(ArcProto xap : arcs)
                tech.arcs.add(xap);
        	for(PrimitiveNode xnp : nodes)
                tech.nodes.add(xnp);
        }

        private void beginCharacters() {
            assert !acceptCharacters;
            acceptCharacters = true;
            assert charBuffer.length() == 0;
        }
        private String endCharacters() {
            assert acceptCharacters;
            String s = charBuffer.toString();
            charBuffer.setLength(0);
            acceptCharacters = false;
            return s;
        }


        ////////////////////////////////////////////////////////////////////
        // Default implementation of the EntityResolver interface.
        ////////////////////////////////////////////////////////////////////

        /**
         * Resolve an external entity.
         *
         * <p>Always return null, so that the parser will use the system
         * identifier provided in the XML document.  This method implements
         * the SAX default behaviour: application writers can override it
         * in a subclass to do special translations such as catalog lookups
         * or URI redirection.</p>
         *
         * @param publicId The public identifier, or null if none is
         *                 available.
         * @param systemId The system identifier provided in the XML
         *                 document.
         * @return The new input source, or null to require the
         *         default behaviour.
         * @exception java.io.IOException If there is an error setting
         *            up the new input source.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.EntityResolver#resolveEntity
         */
        public InputSource resolveEntity(String publicId, String systemId)
        throws IOException, SAXException {
            return null;
        }



        ////////////////////////////////////////////////////////////////////
        // Default implementation of DTDHandler interface.
        ////////////////////////////////////////////////////////////////////


        /**
         * Receive notification of a notation declaration.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass if they wish to keep track of the notations
         * declared in a document.</p>
         *
         * @param name The notation name.
         * @param publicId The notation public identifier, or null if not
         *                 available.
         * @param systemId The notation system identifier.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.DTDHandler#notationDecl
         */
        public void notationDecl(String name, String publicId, String systemId)
        throws SAXException {
//            int x = 0;
        }


        /**
         * Receive notification of an unparsed entity declaration.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to keep track of the unparsed entities
         * declared in a document.</p>
         *
         * @param name The entity name.
         * @param publicId The entity public identifier, or null if not
         *                 available.
         * @param systemId The entity system identifier.
         * @param notationName The name of the associated notation.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.DTDHandler#unparsedEntityDecl
         */
        public void unparsedEntityDecl(String name, String publicId,
                String systemId, String notationName)
                throws SAXException {
//            int x = 0;
        }



        ////////////////////////////////////////////////////////////////////
        // Default implementation of ContentHandler interface.
        ////////////////////////////////////////////////////////////////////


        /**
         * Receive a Locator object for document events.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass if they wish to store the locator for use
         * with other document events.</p>
         *
         * @param locator A locator for all SAX document events.
         * @see org.xml.sax.ContentHandler#setDocumentLocator
         * @see org.xml.sax.Locator
         */
        public void setDocumentLocator(Locator locator) {
            this.locator = locator;
        }

        private void printLocator() {
            System.out.println("publicId=" + locator.getPublicId() + " systemId=" + locator.getSystemId() +
                    " line=" + locator.getLineNumber() + " column=" + locator.getColumnNumber());
        }


        /**
         * Receive notification of the beginning of the document.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the beginning
         * of a document (such as allocating the root node of a tree or
         * creating an output file).</p>
         *
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#startDocument
         */
        public void startDocument()
        throws SAXException {
            if (DEBUG) {
                System.out.println("startDocument");
            }
        }


        /**
         * Receive notification of the end of the document.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end
         * of a document (such as finalising a tree or closing an output
         * file).</p>
         *
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#endDocument
         */
        public void endDocument()
        throws SAXException {
            if (DEBUG) {
                System.out.println("endDocument");
            }
        }


        /**
         * Receive notification of the start of a Namespace mapping.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the start of
         * each Namespace prefix scope (such as storing the prefix mapping).</p>
         *
         * @param prefix The Namespace prefix being declared.
         * @param uri The Namespace URI mapped to the prefix.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#startPrefixMapping
         */
        public void startPrefixMapping(String prefix, String uri)
        throws SAXException {
            if (DEBUG) {
                System.out.println("startPrefixMapping prefix=" + prefix + " uri=" + uri);
            }
        }


        /**
         * Receive notification of the end of a Namespace mapping.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end of
         * each prefix mapping.</p>
         *
         * @param prefix The Namespace prefix being declared.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#endPrefixMapping
         */
        public void endPrefixMapping(String prefix)
        throws SAXException {
            if (DEBUG) {
                System.out.println("endPrefixMapping prefix=" + prefix);
            }
        }


        /**
         * Receive notification of the start of an element.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the start of
         * each element (such as allocating a new tree node or writing
         * output to a file).</p>
         *
         * @param uri The Namespace URI, or the empty string if the
         *        element has no Namespace URI or if Namespace
         *        processing is not being performed.
         * @param localName The local name (without prefix), or the
         *        empty string if Namespace processing is not being
         *        performed.
         * @param qName The qualified name (with prefix), or the
         *        empty string if qualified names are not available.
         * @param attributes The attributes attached to the element.  If
         *        there are no attributes, it shall be an empty
         *        Attributes object.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#startElement
         */
        public void startElement(String uri, String localName,
                String qName, Attributes attributes)
                throws SAXException {
            boolean dump = false;
            XmlKeyword key = xmlKeywords.get(localName);
//            System.out.print("<" + key.name());
            this.attributes = attributes;
            switch (key) {
                case technology:
                    tech.techName = a("name");
                    tech.className = a_("class");
//                    dump = true;
                    break;
                case numMetals:
                    tech.minNumMetals = Integer.parseInt(a("min"));
                    tech.maxNumMetals = Integer.parseInt(a("max"));
                    tech.defaultNumMetals = Integer.parseInt(a("default"));
                    break;
                case scale:
                    tech.scaleValue = Double.parseDouble(a("value"));
                    tech.scaleRelevant = Boolean.parseBoolean(a("relevant"));
                    break;
                case defaultFoundry:
                    tech.defaultFoundry = a("value");
                    break;
                case minResistance:
                    tech.minResistance = Double.parseDouble(a("value"));
                    break;
                case minCapacitance:
                    tech.minCapacitance = Double.parseDouble(a("value"));
                    break;
                case layer:
                    if (curDisplayStyle != null) {
                        curLayerDisplayStyle = curDisplayStyle.newLayer(tech.findLayer(a("name")));
                        curTransparent = 0;
                        curR = curG = curB = 0;
                        patternedOnDisplay = false;
                        patternedOnPrinter = false;
                        Arrays.fill(pattern, 0);
                        curPatternIndex = 0;
//                      EGraphics.Outline outline = null;
                    } else if (curLayerRule != null) {
                        curDistance = new Distance();
                        Distance old = curLayerRule.put(tech.findLayer(a("name")), curDistance);
                        if (old == null)
                            throw new IllegalArgumentException("Duplicate layer " + a("name"));
                    } else {
                        curLayer = tech.newLayer(a("name"));
                        curLayer.function = com.sun.electric.technology.Layer.Function.valueOf(a("fun"));
                        String extraFunStr = a_("extraFun");
                        if (extraFunStr != null) {
                            if (extraFunStr.equals("depletion_heavy"))
                                curLayer.extraFunction = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.HEAVY;
                            else if (extraFunStr.equals("depletion_light"))
                                curLayer.extraFunction = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.LIGHT;
                            else if (extraFunStr.equals("enhancement_heavy"))
                                curLayer.extraFunction = com.sun.electric.technology.Layer.Function.ENHANCEMENT|com.sun.electric.technology.Layer.Function.HEAVY;
                            else if (extraFunStr.equals("enhancement_light"))
                                curLayer.extraFunction = com.sun.electric.technology.Layer.Function.ENHANCEMENT|com.sun.electric.technology.Layer.Function.LIGHT;
                            else
                                curLayer.extraFunction = com.sun.electric.technology.Layer.Function.parseExtraName(extraFunStr);
                        }
                    }
                    break;
                case cifLayer:
                    curLayer.cif = a("cif");
                    break;
                case skillLayer:
                    curLayer.skill = a("skill");
                    break;
                case parasitics:
                    curLayer.resistance = Double.parseDouble(a("resistance"));
                    curLayer.capacitance = Double.parseDouble(a("capacitance"));
                    curLayer.edgeCapacitance = Double.parseDouble(a("edgeCapacitance"));
                    break;
                case pureLayerNode:
                    curLayer.pureLayerNode = new PureLayerNode();
                    curLayer.pureLayerNode.name = a("name");
                    String styleStr = a_("style");
                    curLayer.pureLayerNode.style = styleStr != null ? Type.valueOf(styleStr) : Type.FILLED;
                    curLayer.pureLayerNode.port = a("port");
                    break;
                case arcProto:
                    curArc = new ArcProto();
                    curArc.name = a("name");
                    curArc.function = com.sun.electric.technology.ArcProto.Function.valueOf(a("fun"));
                    break;
                case wipable:
                    curArc.wipable = true;
                    break;
                case curvable:
                    curArc.curvable = true;
                    break;
                case special:
                    curArc.special = true;
                    break;
                case notUsed:
                    if (curArc != null)
                        curArc.notUsed = true;
                    if (curNode != null)
                        curNode.notUsed = true;
                    break;
                case skipSizeInPalette:
                    if (curArc != null)
                        curArc.skipSizeInPalette = true;
                    if (curNode != null)
                        curNode.skipSizeInPalette = true;
                    break;
                case arcLayer:
                    ArcLayer arcLay = new ArcLayer();
                    arcLay.layer = a("layer");
                    curDistance = arcLay.extend;
                    arcLay.style = Type.valueOf(a("style"));
                    curArc.arcLayers.add(arcLay);
                    break;
                case arcPin:
                    curArc.arcPin = new ArcPin();
                    curArc.arcPin.name = a("name");
                    curArc.arcPin.portName = a("port");
                    curArc.arcPin.elibSize = Double.valueOf(a("elibSize"));
                    break;
                case primitiveNode:
                    curNode = new PrimitiveNode();
                    curNode.name = a("name");
                    curNode.function = com.sun.electric.technology.PrimitiveNode.Function.valueOf(a("fun"));
                    break;
                case shrinkArcs:
                    curNode.shrinkArcs = true;
                    break;
                case square:
                    curNode.square = true;
                    break;
                case canBeZeroSize:
                    curNode.canBeZeroSize = true;
                    break;
                case wipes:
                    curNode.wipes = true;
                    break;
                case lockable:
                    curNode.lockable = true;
                    break;
                case edgeSelect:
                    curNode.edgeSelect = true;
                    break;
                case lowVt:
                    curNode.lowVt = true;
                    break;
                case highVt:
                    curNode.highVt = true;
                    break;
                case nativeBit:
                    curNode.nativeBit = true;
                    break;
                case od18:
                    curNode.od18 = true;
                    break;
                case od25:
                    curNode.od25 = true;
                    break;
                case od33:
                    curNode.od33 = true;
                    break;
                case diskOffset:
                    curNode.diskOffset = EPoint.fromLambda(Double.parseDouble(a("x")), Double.parseDouble(a("y")));
                    break;
                case defaultWidth:
                    curDistance = curNode.defaultWidth;
                    break;
                case defaultHeight:
                    curDistance = curNode.defaultHeight;
                    break;
                case nodeBase:
                    double lx = Double.parseDouble(a("lx"));
                    double hx = Double.parseDouble(a("hx"));
                    double ly = Double.parseDouble(a("ly"));
                    double hy = Double.parseDouble(a("hy"));
                    curNode.nodeBase = ERectangle.fromLambda(lx, ly, hx - lx, hy - ly);
                    break;
                case nodeLayer:
                    curNodeLayer = new NodeLayer();
                    curNodeLayer.layer = a("layer");
                    curNodeLayer.style = Type.valueOf(a("style"));
                    String portNum = a_("portNum");
                    if (portNum != null)
                        curNodeLayer.portNum = Integer.parseInt(portNum);
                    String electrical = a_("electrical");
                    if (electrical != null) {
                        if (Boolean.parseBoolean(electrical))
                            curNodeLayer.inElectricalLayers = true;
                        else
                            curNodeLayer.inLayers = true;
                    } else {
                        curNodeLayer.inElectricalLayers = curNodeLayer.inLayers = true;
                    }
                    break;
                case box:
                    if (curNodeLayer != null) {
                        curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.BOX;
                        curNodeLayer.lx.k = da_("klx", -1);
                        curNodeLayer.hx.k = da_("khx", 1);
                        curNodeLayer.ly.k = da_("kly", -1);
                        curNodeLayer.hy.k = da_("khy", 1);
                    }
                    if (curPort != null) {
                        curPort.lx.k = da_("klx", -1);
                        curPort.hx.k = da_("khx", 1);
                        curPort.ly.k = da_("kly", -1);
                        curPort.hy.k = da_("khy", 1);
                    }
                    break;
                case points:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.POINTS;
                    break;
                case multicutbox:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX;
                    curNodeLayer.lx.k = da_("klx", -1);
                    curNodeLayer.hx.k = da_("khx", 1);
                    curNodeLayer.ly.k = da_("kly", -1);
                    curNodeLayer.hy.k = da_("khy", 1);
                    curNodeLayer.sizeRule = a("sizeRule");
                    curNodeLayer.sepRule = a("sepRule");
                    curNodeLayer.sepRule2D = a_("sepRule2D");
                    break;
                case serpbox:
                    curNodeLayer.representation = com.sun.electric.technology.Technology.NodeLayer.BOX;
                    curNodeLayer.lx.k = da_("klx", -1);
                    curNodeLayer.hx.k = da_("khx", 1);
                    curNodeLayer.ly.k = da_("kly", -1);
                    curNodeLayer.hy.k = da_("khy", 1);
                    curNodeLayer.lWidth = Double.parseDouble(a("lWidth"));
                    curNodeLayer.rWidth = Double.parseDouble(a("rWidth"));
                    curNodeLayer.tExtent = Double.parseDouble(a("tExtent"));
                    curNodeLayer.bExtent = Double.parseDouble(a("bExtent"));
                    break;
                case lambdaBox:
                    if (curNodeLayer != null) {
                        curNodeLayer.lx.addLambda(Double.parseDouble(a("klx")));
                        curNodeLayer.hx.addLambda(Double.parseDouble(a("khx")));
                        curNodeLayer.ly.addLambda(Double.parseDouble(a("kly")));
                        curNodeLayer.hy.addLambda(Double.parseDouble(a("khy")));
                    }
                    if (curPort != null) {
                        curPort.lx.addLambda(Double.parseDouble(a("klx")));
                        curPort.hx.addLambda(Double.parseDouble(a("khx")));
                        curPort.ly.addLambda(Double.parseDouble(a("kly")));
                        curPort.hy.addLambda(Double.parseDouble(a("khy")));
                    }
                    break;
                case techPoint:
                    double xm = Double.parseDouble(a("xm"));
                    double xa = Double.parseDouble(a("xa"));
                    double ym = Double.parseDouble(a("ym"));
                    double ya = Double.parseDouble(a("ya"));
                    TechPoint p = new TechPoint(new EdgeH(xm, xa), new EdgeV(ym, ya));
                    if (curNodeLayer != null)
                        curNodeLayer.techPoints.add(p);
                    break;
                case primitivePort:
                    curPort = new PrimitivePort();
                    curPort.name = a("name");
                    break;
                case portAngle:
                    curPort.portAngle = Integer.parseInt(a("primary"));
                    curPort.portRange = Integer.parseInt(a("range"));
                    break;
                case polygonal:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.POLYGONAL;
                    break;
                case serpTrans:
                    curNode.specialType = com.sun.electric.technology.PrimitiveNode.SERPTRANS;
                    curNode.specialValues = new double[6];
                    curSpecialValueIndex = 0;
                    break;
                case minSizeRule:
                    curNode.nodeSizeRule = new NodeSizeRule();
                    curNode.nodeSizeRule.width = Double.parseDouble(a("width"));
                    curNode.nodeSizeRule.height = Double.parseDouble(a("height"));
                    curNode.nodeSizeRule.rule = a("rule");
                    break;
                case spiceTemplate:
                	curNode.spiceTemplate = a("value");
                	break;
                case spiceHeader:
                    curSpiceHeader = new SpiceHeader();
                    curSpiceHeader.level = Integer.parseInt(a("level"));
                    tech.spiceHeaders.add(curSpiceHeader);
                    break;
                case spiceLine:
                    curSpiceHeader.spiceLines.add(a("line"));
                    break;

                case displayStyle:
                    curDisplayStyle = new DisplayStyle();
                    curDisplayStyle.name = a("name");
                    tech.displayStyles.add(curDisplayStyle);
                    break;
                case transparentLayer:
                    curTransparent = Integer.parseInt(a("transparent"));
                    curR = curG = curB = 0;
                    break;
                case transparentColor:
                    curTransparent = Integer.parseInt(a("transparent"));
                    if (curTransparent > 0) {
                        Color color = curDisplayStyle.transparentLayers.get(curTransparent - 1);
                        curR = color.getRed();
                        curG = color.getGreen();
                        curB = color.getBlue();
                    }
                    break;
                case opaqueColor:
                    curR = Integer.parseInt(a("r"));
                    curG = Integer.parseInt(a("g"));
                    curB = Integer.parseInt(a("b"));
                    break;
                case display3D:
                    curLayerDisplayStyle.mode3D = a("mode");
                    curLayerDisplayStyle.factor3D = Double.parseDouble(a("factor"));
                    break;

                case menuPalette:
                    tech.menuPalette = new MenuPalette();
                    tech.menuPalette.numColumns = Integer.parseInt(a("numColumns"));
                    break;
                case menuBox:
                    curMenuBox = new ArrayList<Object>();
                    tech.menuPalette.menuBoxes.add(curMenuBox);
                    break;
                case menuCell:
                    curMenuCell = new MenuCell();
                    curMenuCell.cellName = a("cellName");
                    break;
                case menuNodeInst:
                    curMenuNodeInst = new MenuNodeInst();
                    curMenuNodeInst.protoName = a("protoName");
                    curMenuNodeInst.function =  com.sun.electric.technology.PrimitiveNode.Function.valueOf(a("function"));
                    String rotField = a_("rotation");
                    if (rotField != null) curMenuNodeInst.rotation = Integer.parseInt(rotField);
                    break;
                case menuNodeText:
                    curMenuNodeInst.text = a("text");
                    curMenuNodeInst.fontSize = Double.parseDouble(a("size"));
                    break;
                case rule:
                    String kStr = a_("k");
                    Layer layer = null;
                    Layer layer2 = null;
                    String layerStr = a_("layer");
                    if (layerStr != null) {
                        layer = tech.findLayer(layerStr);
                        assert layer != null;
                    }
                    String layerStr2 = layerStr != null ? a_("layer2") : null;
                    if (layerStr2 != null) {
                        layer2 = tech.findLayer(layerStr2);
                        assert layer2 != null;
                    }
                    curDistance.addRule(a("ruleName"), layer, layer2, kStr != null ? Double.valueOf(kStr) : 1);
                    break;
                case ruleSet:
                    curRuleSet = tech.newRuleSet(a("ruleName"));
                    break;
                case layerRule:
                    curLayerRule = curRuleSet.newLayerRule(a("ruleName"));
                    break;
                case Foundry:
                    curFoundry = new Foundry();
                    curFoundry.name = a("name");
                    tech.foundries.add(curFoundry);
                    break;
                case layerGds:
                    curFoundry.layerGds.put(a("layer"), a("gds"));
                    break;
                case LayerRule:
                case LayersRule:
                case NodeLayersRule:
                case NodeRule:
                    DRCTemplate.parseXmlElement(curFoundry.rules, key.name(), attributes, localName);
                    break;
                default:
                    assert key.hasText;
                    beginCharacters();
//                    System.out.print(">");
                    return;
            }
            assert !key.hasText;
//            System.out.println(">");
            if (dump) {
                System.out.println("startElement uri=" + uri + " localName=" + localName + " qName=" + qName);
                for (int i = 0; i < attributes.getLength(); i++) {
                    System.out.println("\tattribute " + i + " uri=" + attributes.getURI(i) +
                            " localName=" + attributes.getLocalName(i) + " QName=" + attributes.getQName(i) +
                            " type=" + attributes.getType(i) + " value=<" + attributes.getValue(i) + ">");
                }
            }
        }

        private double da_(String attrName, double defaultValue) {
            String s = a_(attrName);
            return s != null ? Double.parseDouble(s) : defaultValue;
        }

        private String a(String attrName) {
            String v = attributes.getValue(attrName);
//            System.out.print(" " + attrName + "=\"" + v + "\"");
            return v;
        }

        private String a_(String attrName) {
            String v = attributes.getValue(attrName);
            if (v == null) return null;
//            System.out.print(" " + attrName + "=\"" + v + "\"");
            return v;
        }


        /**
         * Receive notification of the end of an element.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions at the end of
         * each element (such as finalising a tree node or writing
         * output to a file).</p>
         *
         * @param uri The Namespace URI, or the empty string if the
         *        element has no Namespace URI or if Namespace
         *        processing is not being performed.
         * @param localName The local name (without prefix), or the
         *        empty string if Namespace processing is not being
         *        performed.
         * @param qName The qualified name (with prefix), or the
         *        empty string if qualified names are not available.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#endElement
         */
        public void endElement(String uri, String localName, String qName)
        throws SAXException {
            XmlKeyword key = xmlKeywords.get(localName);
            if (key.hasText) {
                String text = endCharacters();
//                System.out.println(text + "</" + localName + ">");
                switch (key) {
                    case shortName:
                        tech.shortTechName = text;
                        break;
                    case description:
                        tech.description = text;
                        break;
                    case oldName:
                        if (curLayer != null)
                            curLayer.pureLayerNode.oldName = text;
                        if (curArc != null)
                            curArc.oldName = text;
                        if (curNode != null)
                            curNode.oldName = text;
                        break;
                    case extended:
                        curArc.extended = Boolean.parseBoolean(text);
                        break;
                    case fixedAngle:
                        curArc.fixedAngle = Boolean.parseBoolean(text);
                        break;
                    case wipable:
                        curArc.wipable = Boolean.parseBoolean(text);
                        break;
                    case angleIncrement:
                        curArc.angleIncrement = Integer.parseInt(text);
                        break;
                    case antennaRatio:
                        curArc.antennaRatio = Double.parseDouble(text);
                        break;
                    case elibWidthOffset:
                        curArc.elibWidthOffset = Double.parseDouble(text);
                        break;
                    case portTopology:
                        curPort.portTopology = Integer.parseInt(text);
                        break;
                    case portArc:
                        if (curLayer != null && curLayer.pureLayerNode != null)
                            curLayer.pureLayerNode.portArcs.add(text);
                        if (curArc != null && curArc.arcPin != null)
                            curArc.arcPin.portArcs.add(text);
                        if (curPort != null)
                            curPort.portArcs.add(text);
                        break;
                    case specialValue:
                        curNode.specialValues[curSpecialValueIndex++] = Double.parseDouble(text);
                        break;

                    case r:
                        curR = Integer.parseInt(text);
                        break;
                    case g:
                        curG = Integer.parseInt(text);
                        break;
                    case b:
                        curB = Integer.parseInt(text);
                        break;
                    case patternedOnDisplay:
                        patternedOnDisplay = Boolean.parseBoolean(text);
                        break;
                    case patternedOnPrinter:
                        patternedOnPrinter = Boolean.parseBoolean(text);
                        break;
                    case pattern:
                        int p = 0;
                        assert text.length() == 16;
                        for (int j = 0; j < text.length(); j++) {
                            if (text.charAt(text.length() - j - 1) != ' ')
                                p |= (1 << j);
                        }
                        pattern[curPatternIndex++] = p;
                        break;
                    case outlined:
                        outline = EGraphics.Outline.valueOf(text);
                        break;
                    case opacity:
                        opacity = Double.parseDouble(text);
                        break;
                    case foreground:
                        foreground = Boolean.parseBoolean(text);
                        break;

                    case menuArc:
                        curMenuBox.add(tech.findArc(text));
                        break;
                    case menuNode:
                        curMenuBox.add(tech.findNode(text));
                        break;
                    case menuText:
                        curMenuBox.add(text);
                        break;
                    case lambda:
                        curDistance.addLambda(Double.parseDouble(text));
                        break;
                    default:
                        assert false;
                }
                return;
            }
//            System.out.println("</" + localName + ">");
            switch (key) {
                case technology:
                    break;
                case layer:
                    if (curDisplayStyle != null) {
                        assert curPatternIndex == pattern.length;
                        curLayerDisplayStyle.desc = new EGraphics(patternedOnDisplay, patternedOnPrinter, outline, curTransparent,
                            curR, curG, curB, opacity, foreground, pattern.clone());
                        curLayerDisplayStyle = null;
                    } else if (curLayerRule != null) {
                        curDistance = null;
                    } else {
                        curLayer = null;
                    }
                    break;
                case arcProto:
                    tech.arcs.add(curArc);
                    curArc = null;
                    break;
                case primitiveNode:
                    tech.nodes.add(curNode);
                    curNode = null;
                    break;
                case nodeLayer:
                    curNode.nodeLayers.add(curNodeLayer);
                    curNodeLayer = null;
                    break;
                case primitivePort:
                    curNode.ports.add(curPort);
                    curPort = null;
                    break;
                case menuNodeInst:
                	curMenuBox.add(curMenuNodeInst);
                    curMenuNodeInst = null;
                    break;

                case displayStyle:
                    curDisplayStyle = null;
                    break;
                case transparentLayer:
                    while (curTransparent > curDisplayStyle.transparentLayers.size())
                        curDisplayStyle.transparentLayers.add(null);
                    Color oldColor = curDisplayStyle.transparentLayers.set(curTransparent - 1, new Color(curR, curG, curB));
                    assert oldColor == null;
                    break;

                case menuCell:
                	curMenuBox.add("LOADCELL " + curMenuCell.cellName);
//                	curMenuBox.add(curMenuCell);
                    curMenuCell = null;
                    break;

                case ruleSet:
                    curRuleSet = null;
                    break;
                case layerRule:
                    curLayerRule = null;
                    break;

                case numMetals:
                case scale:
                case defaultFoundry:
                case minResistance:
                case minCapacitance:
                case cifLayer:
                case skillLayer:
                case parasitics:
                case pureLayerNode:

                case wipable:
                case curvable:
                case special:
                case notUsed:
                case skipSizeInPalette:
                case arcLayer:
                case arcPin:

                case shrinkArcs:
                case square:
                case canBeZeroSize:
                case wipes:
                case lockable:
                case edgeSelect:
                case lowVt:
                case highVt:
                case nativeBit:
                case od18:
                case od25:
                case od33:
                case diskOffset:

                case defaultWidth:
                case defaultHeight:
                case nodeBase:
                case box:
                case points:
                case multicutbox:
                case serpbox:
                case lambdaBox:
                case techPoint:
                case portAngle:
                case polygonal:
                case serpTrans:
                case minSizeRule:

                case spiceHeader:
                case spiceLine:
                case spiceTemplate:

                case transparentColor:
                case opaqueColor:
                case display3D:

                case menuPalette:
                case menuBox:
                case menuNodeText:
                case rule:
                case Foundry:
                case layerGds:
                case LayerRule:
                case LayersRule:
                case NodeLayersRule:
                case NodeRule:
                    break;
                default:
                    assert false;
            }
        }


        /**
         * Receive notification of character data inside an element.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method to take specific actions for each chunk of character data
         * (such as adding the data to a node or buffer, or printing it to
         * a file).</p>
         *
         * @param ch The characters.
         * @param start The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#characters
         */
        public void characters(char ch[], int start, int length)
        throws SAXException {
            if (acceptCharacters) {
                charBuffer.append(ch, start, length);
            } else {
                boolean nonBlank = false;
                for (int i = 0; i < length; i++) {
                    char c = ch[start + i];
                    nonBlank = nonBlank || c != ' ' && c != '\n';
                }
                if (nonBlank) {
                    System.out.print("characters size=" + ch.length + " start=" + start + " length=" + length + " {");
                    for (int i = 0; i < length; i++)
                        System.out.print(ch[start + i]);
                    System.out.println("}");
                }
            }
        }


        /**
         * Receive notification of ignorable whitespace in element content.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method to take specific actions for each chunk of ignorable
         * whitespace (such as adding data to a node or buffer, or printing
         * it to a file).</p>
         *
         * @param ch The whitespace characters.
         * @param start The start position in the character array.
         * @param length The number of characters to use from the
         *               character array.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#ignorableWhitespace
         */
        public void ignorableWhitespace(char ch[], int start, int length)
        throws SAXException {
//            int x = 0;
        }


        /**
         * Receive notification of a processing instruction.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions for each
         * processing instruction, such as setting status variables or
         * invoking other methods.</p>
         *
         * @param target The processing instruction target.
         * @param data The processing instruction data, or null if
         *             none is supplied.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#processingInstruction
         */
        public void processingInstruction(String target, String data)
        throws SAXException {
//            int x = 0;
        }


        /**
         * Receive notification of a skipped entity.
         *
         * <p>By default, do nothing.  Application writers may override this
         * method in a subclass to take specific actions for each
         * processing instruction, such as setting status variables or
         * invoking other methods.</p>
         *
         * @param name The name of the skipped entity.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ContentHandler#processingInstruction
         */
        public void skippedEntity(String name)
        throws SAXException {
//            int x = 0;
        }



        ////////////////////////////////////////////////////////////////////
        // Default implementation of the ErrorHandler interface.
        ////////////////////////////////////////////////////////////////////


        /**
         * Receive notification of a parser warning.
         *
         * <p>The default implementation does nothing.  Application writers
         * may override this method in a subclass to take specific actions
         * for each warning, such as inserting the message in a log file or
         * printing it to the console.</p>
         *
         * @param e The warning information encoded as an exception.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ErrorHandler#warning
         * @see org.xml.sax.SAXParseException
         */
        public void warning(SAXParseException e)
        throws SAXException {
            System.out.println("warning publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
        }


        /**
         * Receive notification of a recoverable parser error.
         *
         * <p>The default implementation does nothing.  Application writers
         * may override this method in a subclass to take specific actions
         * for each error, such as inserting the message in a log file or
         * printing it to the console.</p>
         *
         * @param e The error information encoded as an exception.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ErrorHandler#warning
         * @see org.xml.sax.SAXParseException
         */
        public void error(SAXParseException e)
        throws SAXException {
//            System.out.println("error publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
//                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
            throw e;
        }


        /**
         * Report a fatal XML parsing error.
         *
         * <p>The default implementation throws a SAXParseException.
         * Application writers may override this method in a subclass if
         * they need to take specific actions for each fatal error (such as
         * collecting all of the errors into a single report): in any case,
         * the application must stop all regular processing when this
         * method is invoked, since the document is no longer reliable, and
         * the parser may no longer report parsing events.</p>
         *
         * @param e The error information encoded as an exception.
         * @exception org.xml.sax.SAXException Any SAX exception, possibly
         *            wrapping another exception.
         * @see org.xml.sax.ErrorHandler#fatalError
         * @see org.xml.sax.SAXParseException
         */
        public void fatalError(SAXParseException e)
        throws SAXException {
//            System.out.println("fatal error publicId=" + e.getPublicId() + " systemId=" + e.getSystemId() +
//                    " line=" + e.getLineNumber() + " column=" + e.getColumnNumber() + " message=" + e.getMessage() + " exception=" + e.getException());
            throw e;
        }

    }

    private static class Writer {
        private static final int INDENT_WIDTH = 4;
        protected final PrintWriter out;
        private int indent;
        protected boolean indentEmitted;

        private Writer(PrintWriter out) {
            this.out = out;
        }

        private void writeTechnology(Technology t) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());

            header();

            pl("");
            out.println("<!--");
            pl(" *");
            pl(" * Electric(tm) VLSI Design System");
            pl(" *");
            pl(" * File: " + t.techName + ".xml");
            pl(" * " + t.techName + " technology description");
            pl(" * Generated automatically from a library");
            pl(" *");
            pl(" * Copyright (c) " + cal.get(Calendar.YEAR) + " Sun Microsystems and Static Free Software");
            pl(" *");
            pl(" * Electric(tm) is free software; you can redistribute it and/or modify");
            pl(" * it under the terms of the GNU General Public License as published by");
            pl(" * the Free Software Foundation; either version 3 of the License, or");
            pl(" * (at your option) any later version.");
            pl(" *");
            pl(" * Electric(tm) is distributed in the hope that it will be useful,");
            pl(" * but WITHOUT ANY WARRANTY; without even the implied warranty of");
            pl(" * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the");
            pl(" * GNU General Public License for more details.");
            pl(" *");
            pl(" * You should have received a copy of the GNU General Public License");
            pl(" * along with Electric(tm); see the file COPYING.  If not, write to");
            pl(" * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,");
            pl(" * Boston, Mass 02111-1307, USA.");
            pl(" */");
            out.println("-->");
            l();

            b(XmlKeyword.technology); a("name", t.techName); a("class", t.className); l();
            a("xmlns", "http://electric.sun.com/Technology"); l();
            a("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance"); l();
            a("xsi:schemaLocation", "http://electric.sun.com/Technology ../../technology/xml/TechnologyParam.xsd"); cl();
            l();

            bcpel(XmlKeyword.shortName, t.shortTechName);
            bcpel(XmlKeyword.description, t.description);
            b(XmlKeyword.numMetals); a("min", t.minNumMetals); a("max", t.maxNumMetals); a("default", t.defaultNumMetals); el();
            b(XmlKeyword.scale); a("value", t.scaleValue); a("relevant", t.scaleRelevant); el();
            b(XmlKeyword.defaultFoundry); a("value", t.defaultFoundry); el();
            b(XmlKeyword.minResistance); a("value", t.minResistance); el();
            b(XmlKeyword.minCapacitance); a("value", t.minCapacitance); el();
//        printlnAttribute("  gateLengthSubtraction", gi.gateShrinkage);
//        printlnAttribute("  gateInclusion", gi.includeGateInResistance);
//        printlnAttribute("  groundNetInclusion", gi.includeGround);
            l();

            comment("**************************************** LAYERS ****************************************");
            for (Layer li: t.layers.values()) {
                writeXml(li);
            }

            comment("******************** ARCS ********************");
            for (ArcProto ai: t.arcs) {
                writeXml(ai);
                l();
            }

            comment("******************** NODES ********************");
            for (PrimitiveNode ni: t.nodes) {
                writeXml(ni);
                l();
            }

            for (SpiceHeader spiceHeader: t.spiceHeaders)
                writeSpiceHeaderXml(spiceHeader);

            for (DisplayStyle displayStyle: t.displayStyles)
                writeDisplayStyleXml(displayStyle);

            writeMenuPaletteXml(t.menuPalette);

            for (RuleSet ruleSet: t.ruleSets.values())
                writeXml(ruleSet);

            for (Foundry foundry: t.foundries)
                writeFoundryXml(foundry);

            el(XmlKeyword.technology);
        }

        private void writeXml(Layer li) {
            String funString = null;
            int funExtra = li.extraFunction;
            if (funExtra != 0) {
                final int deplEnhMask = com.sun.electric.technology.Layer.Function.DEPLETION|com.sun.electric.technology.Layer.Function.ENHANCEMENT;
                if ((funExtra&deplEnhMask) != 0) {
                    funString = com.sun.electric.technology.Layer.Function.getExtraName(funExtra&(deplEnhMask));
                    funExtra &= ~deplEnhMask;
                    if (funExtra != 0)
                        funString += "_" + com.sun.electric.technology.Layer.Function.getExtraName(funExtra);
                } else {
                    funString = com.sun.electric.technology.Layer.Function.getExtraName(funExtra);
                }
            }
            b(XmlKeyword.layer); a("name", li.name); a("fun", li.function.name()); a("extraFun", funString); cl();

            if (li.cif != null && li.cif.length() > 0) {
                b(XmlKeyword.cifLayer); a("cif", li.cif); el();
            }
            if (li.skill != null && li.skill.length() > 0) {
                b(XmlKeyword.skillLayer); a("skill", li.skill); el();
            }

            // write the SPICE information
            if (li.resistance != 0 || li.capacitance != 0 || li.edgeCapacitance != 0) {
                b(XmlKeyword.parasitics); a("resistance", li.resistance); a("capacitance", li.capacitance); a("edgeCapacitance", li.edgeCapacitance); el();
            }
            if (li.pureLayerNode != null) {
                String nodeName = li.pureLayerNode.name;
                Type style = li.pureLayerNode.style;
                String styleStr = style == Type.FILLED ? null : style.name();
                String portName = li.pureLayerNode.port;
                b(XmlKeyword.pureLayerNode); a("name", nodeName); a("style", styleStr); a("port", portName);
                if (li.pureLayerNode.oldName == null && li.pureLayerNode.portArcs.isEmpty()) {
                    el();
                } else {
                    cl();
                    bcpel(XmlKeyword.oldName, li.pureLayerNode.oldName);
                    for (String portArc: li.pureLayerNode.portArcs)
                        bcpel(XmlKeyword.portArc, portArc);
                    el(XmlKeyword.pureLayerNode);
                }
            }
            el(XmlKeyword.layer);
            l();
        }

        private void writeXml(ArcProto ai) {
            b(XmlKeyword.arcProto); a("name", ai.name); a("fun", ai.function.getConstantName()); cl();
            bcpel(XmlKeyword.oldName, ai.oldName);

            if (ai.wipable)
                bel(XmlKeyword.wipable);
            if (ai.curvable)
                bel(XmlKeyword.curvable);
            if (ai.special)
                bel(XmlKeyword.special);
            if (ai.notUsed)
                bel(XmlKeyword.notUsed);
            if (ai.skipSizeInPalette)
                bel(XmlKeyword.skipSizeInPalette);
            bcpel(XmlKeyword.extended, ai.extended);
            bcpel(XmlKeyword.fixedAngle, ai.fixedAngle);
            bcpel(XmlKeyword.angleIncrement, ai.angleIncrement);
            if (ai.antennaRatio != 0)
                bcpel(XmlKeyword.antennaRatio, ai.antennaRatio);
            if (ai.elibWidthOffset != 0)
                bcpel(XmlKeyword.elibWidthOffset, ai.elibWidthOffset);

            for (ArcLayer al: ai.arcLayers) {
                String style = al.style == Type.FILLED ? "FILLED" : "CLOSED";
                b(XmlKeyword.arcLayer); a("layer", al.layer); a("style", style);
                if (al.extend.isEmpty()) {
                    el();
                } else {
                    cl();
                    writeDistance(al.extend);
                    el(XmlKeyword.arcLayer);
                }
            }
            if (ai.arcPin != null) {
                b(XmlKeyword.arcPin); a("name", ai.arcPin.name); a("port", ai.arcPin.portName); a("elibSize", ai.arcPin.elibSize);
                if (ai.arcPin.portArcs.isEmpty()) {
                    el();
                } else {
                    cl();
                    for (String portArc: ai.arcPin.portArcs)
                        bcpel(XmlKeyword.portArc, portArc);
                    el(XmlKeyword.arcPin);
                }
            }
            el(XmlKeyword.arcProto);
        }

        private void writeXml(PrimitiveNode ni) {
            b(XmlKeyword.primitiveNode); a("name", ni.name); a("fun", ni.function.name()); cl();
            bcpel(XmlKeyword.oldName, ni.oldName);

            if (ni.shrinkArcs)
                bel(XmlKeyword.shrinkArcs);
            if (ni.square)
                bel(XmlKeyword.square);
            if (ni.canBeZeroSize)
                bel(XmlKeyword.canBeZeroSize);
            if (ni.wipes)
                bel(XmlKeyword.wipes);
            if (ni.lockable)
                bel(XmlKeyword.lockable);
            if (ni.edgeSelect)
                bel(XmlKeyword.edgeSelect);
            if (ni.skipSizeInPalette)
                bel(XmlKeyword.skipSizeInPalette);
            if (ni.notUsed)
                bel(XmlKeyword.notUsed);
            if (ni.lowVt)
                bel(XmlKeyword.lowVt);
            if (ni.highVt)
                bel(XmlKeyword.highVt);
            if (ni.nativeBit)
                bel(XmlKeyword.nativeBit);
            if (ni.od18)
                bel(XmlKeyword.od18);
            if (ni.od25)
                bel(XmlKeyword.od25);
            if (ni.od33)
                bel(XmlKeyword.od33);

            if (ni.diskOffset != null) {
                b(XmlKeyword.diskOffset); a("x", ni.diskOffset.getLambdaX()); a("y", ni.diskOffset.getLambdaY()); el();
            }

            if (!ni.defaultWidth.isEmpty()) {
                bcl(XmlKeyword.defaultWidth);
                writeDistance(ni.defaultWidth);
                el(XmlKeyword.defaultWidth);
            }

            if (!ni.defaultHeight.isEmpty()) {
                bcl(XmlKeyword.defaultHeight);
                writeDistance(ni.defaultHeight);
                el(XmlKeyword.defaultHeight);
            }

            if (ni.nodeBase != null) {
                double lx = ni.nodeBase.getLambdaMinX();
                double hx = ni.nodeBase.getLambdaMaxX();
                double ly = ni.nodeBase.getLambdaMinY();
                double hy = ni.nodeBase.getLambdaMaxY();
                b(XmlKeyword.nodeBase); a("lx", lx); a("hx", hx); a("ly", ly); a("hy", hy); el();
            }

            for(int j=0; j<ni.nodeLayers.size(); j++) {
                NodeLayer nl = ni.nodeLayers.get(j);
                b(XmlKeyword.nodeLayer); a("layer", nl.layer); a("style", nl.style.name());
                if (nl.portNum != 0) a("portNum", Integer.valueOf(nl.portNum));
                if (!(nl.inLayers && nl.inElectricalLayers))
                    a("electrical", nl.inElectricalLayers);
                cl();
                switch (nl.representation) {
                    case com.sun.electric.technology.Technology.NodeLayer.BOX:
                        if (ni.specialType == com.sun.electric.technology.PrimitiveNode.SERPTRANS) {
                            writeBox(XmlKeyword.serpbox, nl.lx, nl.hx, nl.ly, nl.hy);
                            a("lWidth", nl.lWidth); a("rWidth", nl.rWidth); a("tExtent", nl.tExtent); a("bExtent", nl.bExtent); cl();
                            writeLambdaBox(nl.lx, nl.hx, nl.ly, nl.hy);
                            el(XmlKeyword.serpbox);
                        } else {
                            writeBox(XmlKeyword.box, nl.lx, nl.hx, nl.ly, nl.hy); cl();
                            writeLambdaBox(nl.lx, nl.hx, nl.ly, nl.hy);
                            el(XmlKeyword.box);
                        }
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.POINTS:
                        b(XmlKeyword.points); el();
                        break;
                    case com.sun.electric.technology.Technology.NodeLayer.MULTICUTBOX:
                        writeBox(XmlKeyword.multicutbox, nl.lx, nl.hx, nl.ly, nl.hy);
                        a("sizeRule", nl.sizeRule); a("sepRule", nl.sepRule); a("sepRule2D", nl.sepRule2D);
                        if (nl.lx.isEmpty() && nl.hx.isEmpty() && nl.ly.isEmpty() && nl.hy.isEmpty()) {
                            el();
                        } else {
                            cl();
                            writeLambdaBox(nl.lx, nl.hx, nl.ly, nl.hy);
                            el(XmlKeyword.multicutbox);
                        }
                        break;
                }
                for (TechPoint tp: nl.techPoints) {
                    double xm = tp.getX().getMultiplier();
                    double xa = tp.getX().getAdder();
                    double ym = tp.getY().getMultiplier();
                    double ya = tp.getY().getAdder();
                    b(XmlKeyword.techPoint); a("xm", xm); a("xa", xa); a("ym", ym); a("ya", ya); el();
                }
                el(XmlKeyword.nodeLayer);
            }
            for (int j = 0; j < ni.ports.size(); j++) {
                PrimitivePort pd = ni.ports.get(j);
                b(XmlKeyword.primitivePort); a("name", pd.name); cl();
                b(XmlKeyword.portAngle); a("primary", pd.portAngle); a("range", pd.portRange); el();
                bcpel(XmlKeyword.portTopology, pd.portTopology);

                writeBox(XmlKeyword.box, pd.lx, pd.hx, pd.ly, pd.hy); cl();
                writeLambdaBox(pd.lx, pd.hx, pd.ly, pd.hy);
                el(XmlKeyword.box);

                for (String portArc: pd.portArcs)
                    bcpel(XmlKeyword.portArc, portArc);
                el(XmlKeyword.primitivePort);
            }
            switch (ni.specialType) {
                case com.sun.electric.technology.PrimitiveNode.POLYGONAL:
                    bel(XmlKeyword.polygonal);
                    break;
                case com.sun.electric.technology.PrimitiveNode.SERPTRANS:
                    b(XmlKeyword.serpTrans); cl();
                    for (int i = 0; i < 6; i++) {
                        bcpel(XmlKeyword.specialValue, ni.specialValues[i]);
                    }
                    el(XmlKeyword.serpTrans);
                    break;
            }
            if (ni.nodeSizeRule != null) {
                NodeSizeRule r = ni.nodeSizeRule;
                b(XmlKeyword.minSizeRule); a("width", r.width); a("height", r.height); a("rule", r.rule); el();
            }
            if (ni.spiceTemplate != null)
            {
            	b(XmlKeyword.spiceTemplate); a("value", ni.spiceTemplate); el();
            }

            el(XmlKeyword.primitiveNode);
        }

        private void writeDistance(Distance d) {
            d.writeXml(this, true);
        }

        private void writeBox(XmlKeyword keyword, Distance lx, Distance hx, Distance ly, Distance hy) {
            b(keyword);
            if (lx.k != -1) a("klx", lx.k);
            if (hx.k != 1) a("khx", hx.k);
            if (ly.k != -1) a("kly", ly.k);
            if (hy.k != 1) a("khy", hy.k);
        }

        private void writeLambdaBox(Distance lx, Distance hx, Distance ly, Distance hy) {
            double lxv = lx.getLambda(EMPTY_CONTEXT);
            double hxv = hx.getLambda(EMPTY_CONTEXT);
            double lyv = ly.getLambda(EMPTY_CONTEXT);
            double hyv = hy.getLambda(EMPTY_CONTEXT);
//            if (lxv == 0 && hxv == 0 && lyv == 0 && hyv == 0) return;
            b(XmlKeyword.lambdaBox); a("klx", lxv); a("khx", hxv); a("kly", lyv); a("khy", hyv); el();
        }

        private void writeSpiceHeaderXml(SpiceHeader spiceHeader) {
            b(XmlKeyword.spiceHeader); a("level", spiceHeader.level); cl();
            for (String line: spiceHeader.spiceLines) {
                b(XmlKeyword.spiceLine); a("line", line); el();
            }
            el(XmlKeyword.spiceHeader);
            l();
        }

        private void writeDisplayStyleXml(DisplayStyle displayStyle) {
            b(XmlKeyword.displayStyle); a("name", displayStyle.name); cl();

            if (displayStyle.transparentLayers.size() != 0) {
                comment("Transparent layers");
                for (int i = 0; i < displayStyle.transparentLayers.size(); i++) {
                    Color color = displayStyle.transparentLayers.get(i);
                    b(XmlKeyword.transparentLayer); a("transparent", i + 1); cl();
                    bcpel(XmlKeyword.r, color.getRed());
                    bcpel(XmlKeyword.g, color.getGreen());
                    bcpel(XmlKeyword.b, color.getBlue());
                    el(XmlKeyword.transparentLayer);
                }
                l();
            }

            for (LayerDisplayStyle l: displayStyle.layerStyles.values()) {
                b(XmlKeyword.layer); a("name", l.layer.name); cl();
                EGraphics desc = l.desc;
                if (desc.getTransparentLayer() > 0) {
                    b(XmlKeyword.transparentColor); a("transparent", desc.getTransparentLayer()); el();
                } else {
                    Color color = desc.getColor();
                    b(XmlKeyword.opaqueColor); a("r", color.getRed()); a("g", color.getGreen()); a("b", color.getBlue()); el();
                }

                bcpel(XmlKeyword.patternedOnDisplay, desc.isPatternedOnDisplay());
                bcpel(XmlKeyword.patternedOnPrinter, desc.isPatternedOnPrinter());

                int [] pattern = desc.getPattern();
                for(int j=0; j<16; j++) {
                    String p = "";
                    for(int k=0; k<16; k++)
                        p += (pattern[j] & (1 << (15-k))) != 0 ? 'X' : ' ';
                    bcpel(XmlKeyword.pattern, p);
                }

                if (desc.getOutlined() != null)
                    bcpel(XmlKeyword.outlined, desc.getOutlined().getConstName());
                bcpel(XmlKeyword.opacity, desc.getOpacity());
                bcpel(XmlKeyword.foreground, desc.getForeground());

                // write the 3D information
                if (l.mode3D != null) {
                    b(XmlKeyword.display3D); a("mode", l.mode3D); a("factor", l.factor3D); el();
                }

                el(XmlKeyword.layer);
            }

            el(XmlKeyword.displayStyle);
            l();
        }

        public void writeMenuPaletteXml(MenuPalette menuPalette) {
            if (menuPalette == null) return;
            b(XmlKeyword.menuPalette); a("numColumns", menuPalette.numColumns); cl();
            for (int i = 0; i < menuPalette.menuBoxes.size(); i++) {
                if (i % menuPalette.numColumns == 0)
                    l();
                writeMenuBoxXml(menuPalette.menuBoxes.get(i));
            }
            l();
            el(XmlKeyword.menuPalette);
            l();
        }

        private void writeMenuBoxXml(List<Object> list) {
            b(XmlKeyword.menuBox);
            if (list.size() == 0) {
                el();
                return;
            }
            cl();
            for (Object o: list) {
                if (o instanceof ArcProto) {
                    bcpel(XmlKeyword.menuArc, ((ArcProto)o).name);
                } else if (o instanceof PrimitiveNode) {
                    bcpel(XmlKeyword.menuNode, ((PrimitiveNode)o).name);
                } else if (o instanceof MenuCell) {
                    MenuCell cell = (MenuCell)o;
                    b(XmlKeyword.menuCell); a("cellName", cell.cellName); el();
                } else if (o instanceof MenuNodeInst) {
                    MenuNodeInst ni = (MenuNodeInst)o;
                    b(XmlKeyword.menuNodeInst); a("protoName", ni.protoName); a("function", ni.function.name());
                    if (ni.rotation != 0) a("rotation", ni.rotation);
                    if (ni.text == null) {
                        el();
                    } else {
                        cl();
                        b(XmlKeyword.menuNodeText); a("text", ni.text); a("size", ni.fontSize); el();
                        el(XmlKeyword.menuNodeInst);
                    }
                } else {
                	if (o == null) bel(XmlKeyword.menuText); else
                		bcpel(XmlKeyword.menuText, o);
                }
            }
            el(XmlKeyword.menuBox);
        }

        private void writeXml(RuleSet ruleSet) {
            b(XmlKeyword.ruleSet); a("name", ruleSet.name); cl();
            for (Map.Entry<String,Map<Layer,Distance>> e: ruleSet.layerRules.entrySet())
                writeLayerRuleXml(e.getKey(), e.getValue());
            el(XmlKeyword.ruleSet);
            l();
        }

        private void writeLayerRuleXml(String ruleName, Map<Layer,Distance> sizes) {
            if (sizes.isEmpty()) return;
            b(XmlKeyword.layerRule); a("ruleName", ruleName); cl();
            int maxNameLength = 0;
            for (Layer l: sizes.keySet())
                maxNameLength = Math.max(maxNameLength, l.name.length());
            for (Map.Entry<Layer,Distance> e: sizes.entrySet()) {
                String layerName = e.getKey().name;
                Distance d = e.getValue();
                b(XmlKeyword.layer); a("name", layerName); c();
                s(maxNameLength - layerName.length());
                d.writeXml(this, false);
                el(XmlKeyword.layer);
            }
            el(XmlKeyword.layerRule);
        }

        private void writeFoundryXml(Foundry foundry) {
            b(XmlKeyword.Foundry); a("name", foundry.name); cl();
            l();
            for (Map.Entry<String,String> e: foundry.layerGds.entrySet()) {
                b(XmlKeyword.layerGds); a("layer", e.getKey()); a("gds", e.getValue()); el();
            }
            l();
            for (DRCTemplate rule: foundry.rules)
                DRCTemplate.exportDRCRule(out, rule);
            el(XmlKeyword.Foundry);
        }

        private void header() {
            checkIndent();
            out.print("<?xml"); a("version", "1.0"); a("encoding", "UTF-8"); out.println("?>");
        }

        private void comment(String s) {
            checkIndent();
            out.print("<!-- "); p(s); out.print(" -->"); l();
        }

        /**
         * Print attribute.
         */
        private void a(String name, Object value) {
            checkIndent();
            if (value == null) return;
            out.print(" " + name + "=\"");
            p(value.toString());
            out.print("\"");
        }

        private void bcpe(XmlKeyword key, Object v) {
            if (v == null) return;
            b(key); c(); p(v.toString()); e(key);
        }

        private void bcpel(XmlKeyword key, Object v) {
            if (v == null) return;
            b(key); c(); p(v.toString()); el(key);
        }

        private void bcl(XmlKeyword key) {
            b(key); cl();
        }

        private void bel(XmlKeyword key) {
            b(key); el();
        }

        /**
         * Print text with replacement of special chars.
         */
        private void pl(String s) {
            checkIndent();
            p(s); l();
        }

        /**
         * Print text with replacement of special chars.
         */
        protected void p(String s) {
            assert indentEmitted;
            for (int i = 0; i < s.length(); i++) {
                char c = s.charAt(i);
                switch (c) {
                    case '<':
                        out.print("&lt;");
                        break;
                    case '>':
                        out.print("&gt;");
                        break;
                    case '&':
                        out.print("&amp;");
                        break;
                    case '\'':
                        out.print("&apos;");
                        break;
                    case '"':
                        out.print("quot;");
                        break;
                    default:
                        out.print(c);
                }
            }
        }

        /**
         * Print element name, and indent.
         */
        private void b(XmlKeyword key) {
            checkIndent();
            out.print('<');
            out.print(key.name());
            indent += INDENT_WIDTH;
        }

        private void cl() {
            assert indentEmitted;
            out.print('>');
            l();
        }

        private void c() {
            assert indentEmitted;
            out.print('>');
        }

        private void el() {
            e(); l();
        }

        private void el(XmlKeyword key) {
            e(key); l();
        }

        private void e() {
            assert indentEmitted;
            out.print("/>");
            indent -= INDENT_WIDTH;
        }

        private void e(XmlKeyword key) {
            indent -= INDENT_WIDTH;
            checkIndent();
            out.print("</");
            out.print(key.name());
            out.print(">");
        }

        protected void checkIndent() {
            if (indentEmitted) return;
            s(indent);
            indentEmitted = true;
        }

        protected void s(int numSpaces) {
            for (int i = 0; i < numSpaces; i++)
                out.print(' ');
        }

        /**
         *  Print new line.
         */
        protected void l() {
            out.println();
            indentEmitted = false;
        }
    }

    public static DistanceContext EMPTY_CONTEXT = new DistanceContext() {
        public double getRule(String ruleName) {
            throw new UnsupportedOperationException();
        }
    };

    /**
     * Class to write the XML without multiple lines and indentation.
     * Useful when the XML is to be a single string.
     */
	private static class OneLineWriter extends Writer
	{
		private OneLineWriter(PrintWriter out)
		{
			super(out);
		}

		/**
		 * Print text without replacement of special chars.
		 */
        @Override
		protected void p(String s)
		{
			for (int i = 0; i < s.length(); i++)
				out.print(s.charAt(i));
		}

        @Override
		protected void checkIndent() { indentEmitted = true; }

		/**
		 * Do not print new line.
		 */
        @Override
		protected void l() { indentEmitted = false; }
	}
}
