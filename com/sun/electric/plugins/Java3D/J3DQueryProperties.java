/*
 * $RCSfile: J3DQueryProperties.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL
 * NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF
 * USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR
 * ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
 * REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
 * INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 *
 * $Revision: 1.3 $
 * $Date: 2006/02/01 19:53:42 $
 * $State: Exp $
 */
package com.sun.electric.plugins.Java3D;

import java.util.*;
import javax.media.j3d.*;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsConfiguration;

/**
 * This class is used to test hardware acceleration available in different platforms.
 * Included by Gilda Garreton
 */
public class J3DQueryProperties {
    private static void printProps(Map map, String[] propList) {
        try{
            // Create an alphabetical list of keys
            List<String> keyList = new ArrayList<String>(map.keySet());
            Collections.sort(keyList);
            Iterator<String> it;

            // Collection used to remember the properties we've already
            // printed, so we don't print them twice
            HashSet<String> hs = new HashSet<String>();

            // Print out the values for the caller-specified properties
            String key;
            for (int i = 0; i < propList.length; i++) {
                int len = propList[i].length();
                int idxWild = propList[i].indexOf('*');
                if (idxWild < 0) {
                    key = propList[i];
                    if (!hs.contains(key)) {
                        System.out.println(key + " = " + map.get(key));
                        hs.add(key);
                    }
                }
                else if (idxWild == len-1) {
                    String pattern = propList[i].substring(0, len-1);
                    it = keyList.iterator();
                    while (it.hasNext()) {
                        key = it.next();
                        if (key.startsWith(pattern) && !hs.contains(key)) {
                            System.out.println(key + " = " + map.get(key));
                            hs.add(key);
                        }
                    }
                }
                else {
                    System.out.println(propList[i] +
                                       " = ERROR: KEY WITH EMBEDDED WILD CARD IGNORED");
                }
            }

            // Print out the values for those properties not already printed
            it = keyList.iterator();
            while (it.hasNext()) {
                key = it.next();
                if (!hs.contains(key)) {
                    System.out.println(key + " = " + map.get(key));
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void queryHardwareAcceleration() {
        Map vuMap = VirtualUniverse.getProperties();
        final String[] vuPropList = {
            "j3d.version",
            "j3d.vendor",
            "j3d.specification.version",
            "j3d.specification.vendor",
            "j3d.*"
            // Just print all other properties in alphabetical order
        };

        printProps(vuMap, vuPropList);
        System.out.println();

        GraphicsConfigTemplate3D template = new GraphicsConfigTemplate3D();

        /* We need to set this to force choosing a pixel format
           that support the canvas.
        */
        template.setStereo(GraphicsConfigTemplate3D.PREFERRED);
        template.setSceneAntialiasing(GraphicsConfigTemplate3D.PREFERRED);

        GraphicsConfiguration config =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getBestConfiguration(template);

        Map c3dMap = new Canvas3D(config).queryProperties();
        final String[] c3dPropList = {
            "native.*",
            "doubleBufferAvailable",
            "stereoAvailable",
            "sceneAntialiasing*",
            "compressedGeometry.majorVersionNumber",
            "compressedGeometry.minorVersionNumber",
            "compressedGeometry.*",
            "textureUnitStateMax",
            "textureWidthMax",
            "textureHeightMax",
            // Just print all other properties in alphabetical order
        };

        printProps(c3dMap, c3dPropList);
    }
}
