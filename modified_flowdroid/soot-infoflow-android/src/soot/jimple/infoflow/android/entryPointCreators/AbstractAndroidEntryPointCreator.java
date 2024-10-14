package soot.jimple.infoflow.android.entryPointCreators;

import java.util.*;

import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Jimple;
import soot.jimple.NopStmt;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.manifest.IManifestHandler;
import soot.jimple.infoflow.cfg.FlowDroidEssentialMethodTag;
import soot.jimple.infoflow.entryPointCreators.BaseEntryPointCreator;
import soot.jimple.infoflow.util.SystemClassHandler;

public abstract class AbstractAndroidEntryPointCreator extends BaseEntryPointCreator {

    protected AndroidEntryPointUtils entryPointUtils = null;

    protected IManifestHandler manifest;

    public AbstractAndroidEntryPointCreator(IManifestHandler manifest) {
        this.manifest = manifest;
    }

    @Override
    public SootMethod createDummyMain() {
        // Initialize the utility class
        this.entryPointUtils = new AndroidEntryPointUtils();

        return super.createDummyMain();
    }

    protected Stmt searchAndBuildMethod(String subsignature, SootClass currentClass, Local classLocal) {
        return searchAndBuildMethod(subsignature, currentClass, classLocal, Collections.<SootClass>emptySet());
    }

    protected Stmt searchAndBuildMethod(String subsignature, SootClass currentClass, Local classLocal,
                                        Set<SootClass> parentClasses) {
        if (currentClass == null || classLocal == null)
            return null;

        SootMethod method = findMethod(currentClass, subsignature);
        if (method == null)
            return null;

        // If the method is in one of the predefined Android classes, it cannot
        // contain custom code, so we do not need to call it
        if (AndroidEntryPointConstants.isLifecycleClass(method.getDeclaringClass().getName()))
            return null;

        // If this method is part of the Android framework, we don't need to
        // call it
        if (SystemClassHandler.v().isClassInSystemPackage(method.getDeclaringClass()))
            return null;

        assert method.isStatic() || classLocal != null
                : "Class local was null for non-static method " + method.getSignature();

        // write Method
        return buildMethodCall(method, classLocal, parentClasses);
    }

    protected boolean createPlainMethodCall(Local classLocal, SootMethod currentMethod) {
        // Do not create calls to lifecycle methods which we handle explicitly
        if (AndroidEntryPointConstants.getServiceLifecycleMethods().contains(currentMethod.getSubSignature()))
            return false;

        NopStmt beforeStmt = Jimple.v().newNopStmt();
        NopStmt thenStmt = Jimple.v().newNopStmt();
        body.getUnits().add(beforeStmt);
        createIfStmt(thenStmt);
        buildMethodCall(currentMethod, classLocal);

        body.getUnits().add(thenStmt);
        createIfStmt(beforeStmt);
        return true;
    }

    public void setEntryPointUtils(AndroidEntryPointUtils entryPointUtils) {
        this.entryPointUtils = entryPointUtils;
    }

    /**
     * Creates instance of the given classes
     *
     * @param classes The classes of which to create instances
     */
    protected void createClassInstances(Collection<SootClass> classes) {
        for (SootClass callbackClass : classes) {
            NopStmt thenStmt = Jimple.v().newNopStmt();
            createIfStmt(thenStmt);
            Local l = localVarsForClasses.get(callbackClass);
            if (l == null) {
                l = generateClassConstructor(callbackClass);
                if (l != null)
                    localVarsForClasses.put(callbackClass, l);
            }
            body.getUnits().add(thenStmt);
        }
    }

    protected void handleDynamicEntryPoints(String component) {
        DynamicCallGraphAnalyzer dcga = DynamicCallGraphAnalyzer.v();
        if (dcga.isLoaded()) {
            if (!dcga.getEntryPointsForComponent(component).isEmpty()) {
                for (String ep : dcga.getEntryPointsForComponent(component)) {
                    try {
                        SootMethod sm = Scene.v().getMethod(ep);
                        buildEntryPointCall(sm);
                    } catch (Exception e) {
                        //do nothing for now
                    }
                }
            }
        }
    }

    protected void buildEntryPointCall(SootMethod methodToCall) {
        if (methodToCall == null) {
            return;
        }

        SootClass sc = methodToCall.getDeclaringClass();

        Local localVal = generateClassConstructor(sc);
        if (localVal == null) {
            return;
        }

        final InvokeExpr invokeExpr;
        final Jimple jimple = Jimple.v();
        List<Value> args = new LinkedList<Value>();
        if (methodToCall.getParameterCount() > 0) {
            for (Type tp : methodToCall.getParameterTypes()) {
                Set<SootClass> constructionStack = new HashSet<SootClass>();
                args.add(getValueForType(tp, constructionStack, Collections.<SootClass>emptySet()));
            }

            if (methodToCall.isStatic()) {
                invokeExpr = jimple.newStaticInvokeExpr(methodToCall.makeRef(), args);
            } else {
                if (methodToCall.isConstructor()) {
                    invokeExpr = jimple.newSpecialInvokeExpr(localVal, methodToCall.makeRef(), args);
                } else if (methodToCall.getDeclaringClass().isInterface()) {
                    invokeExpr = jimple.newInterfaceInvokeExpr(localVal, methodToCall.makeRef(), args);
                } else {
                    invokeExpr = jimple.newVirtualInvokeExpr(localVal, methodToCall.makeRef(), args);
                }
            }
        } else {
            if (methodToCall.isStatic()) {
                invokeExpr = jimple.newStaticInvokeExpr(methodToCall.makeRef());
            } else {
                if (methodToCall.isConstructor()) {
                    invokeExpr = jimple.newSpecialInvokeExpr(localVal, methodToCall.makeRef());
                } else if (methodToCall.getDeclaringClass().isInterface()) {
                    invokeExpr = jimple.newInterfaceInvokeExpr(localVal, methodToCall.makeRef(), args);
                } else {
                    invokeExpr = jimple.newVirtualInvokeExpr(localVal, methodToCall.makeRef());
                }
            }
        }

        Stmt stmt;
        if (!(methodToCall.getReturnType() instanceof VoidType)) {
            Local returnLocal = generator.generateLocal(methodToCall.getReturnType());
            stmt = jimple.newAssignStmt(returnLocal, invokeExpr);
        } else {
            stmt = jimple.newInvokeStmt(invokeExpr);
        }
        body.getUnits().add(stmt);
    }
}
