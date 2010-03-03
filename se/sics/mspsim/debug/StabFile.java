package se.sics.mspsim.debug;

import java.util.ArrayList;
import java.util.Iterator;

import se.sics.mspsim.util.ELFDebug;
import se.sics.mspsim.util.Utils;
import se.sics.mspsim.util.ELFDebug.Stab;

public class StabFile {

    public int startAddress;

    public String path;
    public String file;

    public int stabIndex;
    
    private ArrayList<StabFunction> functions = new ArrayList<StabFunction>();
    private StabFunction lastFunction;
    
    public void handleStabs(Stab[] stabs) {
        int i = stabIndex;
        while(i < stabs.length) {
            ELFDebug.Stab stab = stabs[i];
            switch(stab.type) {
            case ELFDebug.N_SO:
                if (stab.value != startAddress) {
                    return;
                }
                if (stab.data.length() > 0) {
                    if (path == null) {
                        path = stab.data;
                    } else if (file == null) {
                        file = stab.data;
                    }
                }
                i++;
                break;
            case ELFDebug.N_FUN:
                i += addFunction(i, stabs);
                break;
            default:
                i++;
            }
        }
    }
    
    private int addFunction(int i, Stab[] stabs) {
        int index = i;
        Stab stab = stabs[index];
        /* name:ReturnType */
        if (stab.data.length() == 0) {
           /* just ens last function */
           if (lastFunction != null) {
               lastFunction.endAddress = lastFunction.startAddress + stab.value;
           }
           return 1;
        }
        StabFunction fun = new StabFunction();
        functions.add(fun);
        lastFunction = fun;
        String[] fname = stab.data.split(":");
        fun.name = fname[0];
        fun.returnType = fname[1];
        fun.startAddress = stab.value;
        fun.startLine = stab.desc;
        index++;

        while (index < stabs.length && isParam(stabs[index])) {
            fun.addParameter(stabs[index]);
            index++;
        }

        return index - i;
    }

    private boolean isParam(Stab stab) {
        return (stab.type == ELFDebug.N_REG_PARAM ||
            stab.type == ELFDebug.N_VAR_PARAM);
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("File: " + path + file + " starts at: " + startAddress + "\n");
        for (int i = 0; i < functions.size(); i++) {
            sb.append("  ").append(functions.get(i)).append("\n");
        }
        return sb.toString();
    }
}
