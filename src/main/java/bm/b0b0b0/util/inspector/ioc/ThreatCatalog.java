package bm.b0b0b0.util.inspector.ioc;

import java.util.regex.Pattern;

public final class ThreatCatalog {

    public static final String[] MALICIOUS_HOSTS = {
            "api-bstats.online",
            "bstats.xyz",
            "bstats.co",
            "panel.bstats.co",
            "fukmoze.xyz",
            "client.hostflow.eu",
            "hostflow.eu",
            "notspecialovskiy",
            "31.76.21.197",
            "83.143.112.177",
    };

    public static final String[] MALICIOUS_URL_FRAGMENTS = {
            "adod_bstats.jar",
            "/domain.list",
            "plugins/bstats.jar",
            "plugins/bStats/.bstats.lock",
            "basher-dl",
            "basher-spread",
    };

    public static final String[] SAFE_HOST_MARKERS = {
            "bstats.org",
            "spigotmc.org",
            "hub.spigotmc.org",
            "bukkit.org",
            "dev.bukkit.org",
            "spiget.org",
            "api.spiget.org",
            "papermc.io",
            "purpurmc.org",
            "leavesmc.org",
            "maven.org",
            "maven.apache.org",
            "mvnrepository.com",
            "repo1.maven.org",
            "repo.maven.apache.org",
            "jitpack.io",
            "sonatype.org",
            "oss.sonatype.org",
            "repo.codemc.org",
            "repo.dmulloy2.net",
            "repo.essentialsx.net",
            "essentialsx.net",
            "ess3.net",
            "luckperms.net",
            "crafthead.net",
            "dmulloy2.net",
            "codemc.io",
            "yaml.org",
            "git.io",
            "breezewiki.com",
            "iban.com",
            "rdiff-backup.net",
            "repo.md-5.net",
            "repo.minebench.de",
            "maven.enginehub.org",
            "repository.jboss.org",
            "jira.jboss.org",
            "mojang.com",
            "minecraft.net",
            "minecraftservices.com",
            "api.minecraftservices.com",
            "sessionserver.mojang.com",
            "ely.by",
            "github.com",
            "bitbucket.org",
            "apache.org",
            "gnu.org",
            "mozilla.org",
            "w3.org",
            "jboss.org",
            "javassist.org",
            "objectweb.org",
            "rubukkit.org",
            "gamepedia.com",
            "fandom.com",
            "minecraftwiki.net",
            "minecraft.fandom.com",
            "bukkit.fandom.com",
            "leymooo.me",
            "cubekrowd.net",
            "mineacademy.org",
            "docs.mineacademy.org",
            "builtbybit.com",
            "api.builtbybit.com",
            "ip-api.com",
            "mineskin.org",
            "api.mineskin.org",
            "citizensnpcs.co",
            "advntr.dev",
            "docs.advntr.dev",
            "appspot.com",
            "googleapis.com",
            "oracle.com",
            "openjdk.org",
            "wordpress.com",
            "imgur.com",
            "i.imgur.com",
            "bintray.com",
            "jcenter.bintray.com",
            "jsonformatter.org",
            "matejpacan.com",
            "mcheads.ru",
            "minotar.net",
            "crafatar.com",
            "visage.surgeplay.com",
            "regex101.com",
            "soltoder.com",
            "youtube.com",
            "youtu.be",
            "google.com",
            "wikipedia.org",
            "stackoverflow.com",
            "curseforge.com",
            "modrinth.com",
            "hangar.papermc.io",
            "namemc.com",
            "spark.lucko.me",
            "lucko.me",
            "goo.gl",
            "minecraft-heads.com",
            "helpch.at",
            "wiki.helpch.at",
    };

    public static final String[] USER_CONTENT_HOST_MARKERS = {
            "pastes.dev",
            "pastebin.com",
            "hastebin.com",
            "paste.ee",
            "rentry.co",
            "rentry.org",
            "dpaste.org",
            "ghostbin.com",
            "paste.helpch.at",
    };

    public static final String[] RESERVED_IP_PREFIXES = {
            "0.", "10.", "127.", "129.", "169.254.", "192.168.", "224.", "239.", "255.",
            "172.16.", "172.17.", "172.18.", "172.19.", "172.20.", "172.21.",
            "172.22.", "172.23.", "172.24.", "172.25.", "172.26.", "172.27.",
            "172.28.", "172.29.", "172.30.", "172.31.",
    };

    public static final String[] RUNTIME_MARKERS = {
            "java/lang/Runtime.exec",
            "java/lang/ProcessBuilder",
    };

    public static final Pattern URL_PATTERN =
            Pattern.compile("https?://[A-Za-z0-9._~:/?#\\[\\]@!$&'()*+,;=%-]+");
    public static final Pattern DISCORD_WEBHOOK =
            Pattern.compile("https?://(?:ptb\\.|canary\\.)?discord(?:app)?\\.com/api(?:/v\\d+)?/webhooks/\\d+/[\\w.-]+(?:\\?thread_id=\\d+)?");
    public static final Pattern DISCORD_INVITE =
            Pattern.compile("https?://(?:ptb\\.|canary\\.)?discord(?:app)?\\.com/invite/[\\w-]+|https?://discord\\.gg/[\\w-]+");
    public static final Pattern TELEGRAM_BOT =
            Pattern.compile("https?://api\\.telegram\\.org/bot\\d+:[A-Za-z0-9_-]+");
    public static final Pattern SLACK_WEBHOOK =
            Pattern.compile("https?://hooks\\.slack\\.com/services/[A-Z0-9]+/[A-Z0-9]+/[A-Za-z0-9]+");
    public static final Pattern RAW_IPV4 =
            Pattern.compile("\\b(?:(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(?:25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)(?::\\d{1,5})?\\b");

    private ThreatCatalog() {
    }
}
