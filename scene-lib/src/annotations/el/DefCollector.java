package annotations.el;

import checkers.nullness.quals.*;
import checkers.javari.quals.*;

import java.io.*;
import java.util.*;

import annotations.*;
import annotations.field.*;
import annotations.util.coll.*;

/**
 * A DefCollector supplies a visitor for the annotation definitions in an
 * AScene.  First, call the DefCollector constructor passing the AScene.
 * Then, call the visit method.
 * This class exists primarily for the benefit of
 * {@link annotations.io.IndexFileWriter#write(AScene, Writer)}.
 */
public abstract class DefCollector {

    // The set of all definitions in the Scene, which the visitor iterates
    // over.
    private final Set<AnnotationDef> defs;

    /**
     * Constructs a new {@link DefCollector}, which immediately collects all
     * the definitions from annotations the given scene.  Next call
     * {@link #visit} to have the definitions passed back to you in topological
     * order.  If the scene contains two irreconcilable definitions of the
     * same annotation type, a {@link DefException} is thrown.
     */
    public DefCollector(/*@ReadOnly*/ AScene s)
            throws DefException {
        defs = new LinkedHashSet<AnnotationDef>();
        collect(s);
    }

    // The name "collect" in the methods below means to insert or add to
    // the the DefCollector.  "Insert" or "add" would have been better, but
    // at least the methods are private.

    private AnnotationDef getDef(String name) {
        for (AnnotationDef def : defs) {
            if (def.name.equals(name)) {
                return def;
            }
        }
        return null;
    }


    private void collect(/*@ReadOnly*/ AScene s)
            throws DefException {
        for (/*@ReadOnly*/ AElement p : s.packages.values())
            collect(p);
        for (/*@ReadOnly*/ AClass c : s.classes.values())
            collect(c);
    }

    private void collect(AnnotationDef d) throws DefException {
        if (defs.contains(d)) {
            return;
        }

        // define the fields first
        for (AnnotationFieldType aft : d.fieldTypes.values())
            if (aft instanceof AnnotationAFT)
                collect(((AnnotationAFT) aft).annotationDef);

        AnnotationDef oldD = getDef(d.name);
        if (oldD == null)
            defs.add(d);
        else {
            AnnotationDef ud = AnnotationDef.unify(oldD, d);
            if (ud != null) {
                defs.remove(oldD);
                defs.add(ud);
            } else {
                throw new DefException(d.name);
            }
        }
    }

    private void collect(/*@ReadOnly*/ AElement e)
            throws DefException {
        for (Annotation tla : e.tlAnnotationsHere) {
            AnnotationDef tld = tla.def;
            if (defs.contains(tld)) {
                return;
            }

            AnnotationDef d = tld;
            collect(d);

            AnnotationDef oldTld = getDef(d.name);
            if (oldTld == null)
                defs.add(tld);
            else {
                AnnotationDef utld = AnnotationDef.unify(oldTld, tld);
                if (utld != null) {
                    defs.remove(oldTld);
                    defs.add(utld);
                } else {
                    throw new DefException(d.name);
                }
            }
        }
    }

    private void collect(/*@ReadOnly*/ ATypeElement e)
            throws DefException {
        collect((/*@ReadOnly*/ AElement) e);
        for (AElement it : e.innerTypes.values())
            collect(it);
    }

    private void collect(/*@ReadOnly*/ AMethod m)
            throws DefException {
        for (ATypeElement b : m.bounds.values())
            collect(b);
        collect((/*@ReadOnly*/ ATypeElement) m);
        collect(m.receiver);
        for (ATypeElement p : m.parameters.values())
            collect(p);
        for (ATypeElement l : m.locals.values())
            collect(l);
        for (ATypeElement tc : m.typecasts.values())
            collect(tc);
        for (ATypeElement i : m.instanceofs.values())
            collect(i);
        for (ATypeElement n : m.news.values())
            collect(n);
    }

    private void collect(/*@ReadOnly*/ AClass c)
            throws DefException {
        collect((/*@ReadOnly*/ AElement) c);
        for (ATypeElement b : c.bounds.values())
            collect(b);
        for (AMethod m : c.methods.values())
            collect(m);
        for (ATypeElement f : c.fields.values())
            collect(f);
    }

    /**
     * Override this method to perform some sort of subclass-specific
     * processing on the given {@link AnnotationDef}.
     */
    protected abstract void visitAnnotationDef(AnnotationDef d);

    /**
     * Calls {@link #visitAnnotationDef} on the definitions collected from
     * the scene that was passed to the constructor.  Visiting is done in
     * topological order:  if the definition of <code>A</code> contains a
     * subannotation of type <code>B</code>, then <code>B</code> is
     * guaranteed to be visited before <code>A</code>.
     */
    public final void visit() {
        for (AnnotationDef d : defs) {
            visitAnnotationDef(d);
        }
    }
}
