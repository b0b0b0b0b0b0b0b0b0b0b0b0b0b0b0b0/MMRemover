package bm.b0b0b0.util.dev;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class ServerJvmVerify {

    private static final String[][] TARGETS = {
            {"MobCoinsPlus.jar", "com.chup.mobcoinsplus.Main"},
            {"MLSAC-1.0-b42-java17.jar", "wtf.mlsac.Main"},
            {"MyCommand.jar", "it.emmerrei.mycommand.Main"},
            {"WorldGuard.jar", "com.sk89q.worldguard.bukkit.WorldGuardPlugin"},
            {"AntiWorldFly-2.5.4.jar", "com.hm.antiworldfly.AntiWorldFly"},
            {"Vulcan-2.9.7-cracked.jar", "me.frep.vulcan.spigot.VulcanPlugin"},
            {"SIMSpecAlert-1.1-SQD.jar", "surc.simbay.simspecalert.main.SIMSpecAlert"},
            {"FastAsyncWorldEdit-Bukkit-2.4.8.jar", "com.sk89q.worldedit.bukkit.WorldEditPlugin"},
            {"ProtocolLib.jar", "com.comphenix.protocol.ProtocolLib"},
            {"EssentialsX-2.21.1-dev+12-8455212.jar", "com.earth2me.essentials.Essentials"},
            {"LPX  v3.7.11.jar", "it.ytnoos.lpx.AD"},
            {"ExploitFixer v3.3.6.jar", "com.arkflame.exploitfixer.ExploitFixer"},
    };

    public static void main(String[] args) throws Exception {
        File out = new File(System.getProperty("user.dir"), "out");
        List<String> errors = new ArrayList<>();

        for (String[] t : TARGETS) {
            File jar = new File(out, t[0]);
            if (!jar.isFile()) {
                errors.add(t[0] + ": missing in out/");
                continue;
            }
            try {
                JvmVerify.verifyJarClass(jar, t[1]);
                System.out.println("OK: " + t[0]);
            } catch (VerifyError ve) {
                errors.add(t[0] + " :: VerifyError: " + ve.getMessage());
            } catch (Throwable ex) {
                errors.add(t[0] + " :: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
            }
        }

        if (errors.isEmpty()) {
            System.out.println("Server JVM verify: all " + TARGETS.length + " passed");
        } else {
            System.err.println("Server JVM verify FAIL: " + errors.size());
            errors.forEach(System.err::println);
            System.exit(1);
        }
    }
}
