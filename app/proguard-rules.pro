# kotlinx.serialization ships its own R8 rules for the generated serializers.
# Keep the save-file classes intact so old save files always load after an update.
-keep class uz.nucleo.game.** { *; }
