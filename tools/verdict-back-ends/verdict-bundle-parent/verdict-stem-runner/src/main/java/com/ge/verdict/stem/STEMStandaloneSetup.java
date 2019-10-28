/* See LICENSE in project directory */
package com.ge.verdict.stem;

import com.ge.research.sadl.SADLStandaloneSetup;
import com.google.inject.Guice;
import com.google.inject.Injector;

/** Standalone setup for the Verdict STEM runner outside of the Eclipse platform. */
public class STEMStandaloneSetup extends SADLStandaloneSetup {

    @Override
    public Injector createInjector() {
        return Guice.createInjector(new STEMRuntimeModule());
    }
}
