package com.mineblock11.spoofer.config;

import com.mineblock11.spoofer.types.ModelSpoofState;
import com.mineblock11.spoofer.types.SpoofScope;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.impl.controller.EnumControllerBuilderImpl;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SpooferConfigScreen implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> createConfig().generateScreen(parent);
    }

    private YetAnotherConfigLib createConfig() {
        return YetAnotherConfigLib.createBuilder()
                //LIES! LIES! THIS TITLE SERVES NO PURPOSE! IT'S ONLY USED FOR NARRATION! AAAAAAAAAAAAAAAHH NIGHTMARE NIGHTMARE NIGHTMARE!!!!! I HATE THE ANTICHRIST!!
                //ISXANDER WHY WOULD YOU DO THIS TO ME
                //GOD WHY HAVE YOU ALLOWED ME TO LEARN COMPUTER SCIENCE
                //AND WHY DID YOU CURSE ME WITH SUCH STUPIDITY
                .title(Text.literal("Spoofer Config"))
                .category(createMainCategory())
                .build();
    }

    private ConfigCategory createMainCategory() {
        return ConfigCategory.createBuilder()
                .name(Text.literal("Spoofer Settings"))
                .tooltip(Text.of("Settings for the Spoofer mod."))
                .groups(createScopeOptions())
                .build();
    }

    private HashSet<OptionGroup> createScopeOptions() {
        HashSet<OptionGroup> groups = new HashSet<>();

        Set<SpooferOptions> rawScopes = SpooferConfig.scopes;
        Set<SpooferOptions> sortedScopes = new HashSet<>();
        for (SpooferOptions opts : rawScopes) {
            if (opts.scope == SpoofScope.GLOBAL) {
                sortedScopes.add(opts);
                rawScopes.remove(opts);
                break;
            }
        }
        sortedScopes.addAll(rawScopes);

        for (SpooferOptions opts : sortedScopes) {
            groups.add(OptionGroup.createBuilder()
                    .name(Text.literal("Scope: " + opts.scope.name() + (opts.server.map(s -> " - " + s).orElse(""))))
                    .description(OptionDescription.of(Text.of("Which server should the settings apply to? If \"GLOBAL\", you can change the base settings that all servers use as a default.")))
                    .option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Chat Feedback"))
                            .description(OptionDescription.of(Text.of("Should Spoofer give you feedback in chat when you use it's commands?")))
                            .binding(
                                    SpooferConfig.getDefaultScope().ENABLE_SPOOF_FEEDBACK, // default value
                                    () -> opts.ENABLE_SPOOF_FEEDBACK,
                                    value -> opts.ENABLE_SPOOF_FEEDBACK = value
                            )
                            .controller(TickBoxControllerBuilder::create)
                            .build()
                    ).option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Chat Spoof"))
                            .description(OptionDescription.of(Text.of("If enabled, Spoofer changes the names in chat of the spoofed players to their fake name")))
                            .binding(
                                    SpooferConfig.getDefaultScope().ENABLE_CHAT_SPOOF, // default value
                                    () -> opts.ENABLE_CHAT_SPOOF,
                                    value -> opts.ENABLE_CHAT_SPOOF = value
                            )
                            .controller(TickBoxControllerBuilder::create)
                            .build()
                    ).option(Option.<Boolean>createBuilder()
                            .name(Text.literal("TAB Spoof"))
                            .description(OptionDescription.of(Text.of("If enabled, Spoofer changes the names in the player list (TAB) of the spoofed players to their fake name")))
                            .binding(
                                    SpooferConfig.getDefaultScope().ENABLE_TAB_SPOOF, // default value
                                    () -> opts.ENABLE_TAB_SPOOF,
                                    value -> opts.ENABLE_TAB_SPOOF = value
                            )
                            .controller(TickBoxControllerBuilder::create)
                            .build()
                    ).option(Option.<ModelSpoofState>createBuilder()
                            .name(Text.literal("Player Model Spoof"))
                            .description(OptionDescription.of(Text.of("If enabled, Spoofer can change the skin width (slim/classic) of spoofed players to match their skin. Stretch means that all spoofed players will be Classic width. Model means that their skin width will match the spoofed skin's width")))
                            .binding(
                                    SpooferConfig.getDefaultScope().MODEL_SPOOF, // default value
                                    () -> opts.MODEL_SPOOF,
                                    value -> opts.MODEL_SPOOF = value
                            )
                            .controller((option) -> EnumControllerBuilder.create(option).enumClass(ModelSpoofState.class))
                            .build()
                    ).option(Option.<String>createBuilder()
                            .name(Text.literal("Spoof All Players"))
                            .description(OptionDescription.of(Text.of("If field is not \"<disabled>\", Spoofer automatically spoofs new joins as what you typed below, plus a identifier.")))
                            .binding(
                                    Objects.toString(SpooferConfig.getDefaultScope().SPOOF_NEW_PLAYERS_PREFIX, "<disabled>"), // default value
                                    () -> opts.SPOOF_NEW_PLAYERS_PREFIX,
                                    value -> opts.SPOOF_NEW_PLAYERS_PREFIX = value
                            )
                            .controller(StringControllerBuilder::create)
                            .build()
                    ).option(Option.<Boolean>createBuilder()
                            .name(Text.literal("Spoof All Players: Keep Skins?"))
                            .description(OptionDescription.of(Text.of("If enabled and above option is set, automatically spoofed players also get their skin spoofed.")))
                            .binding(
                                    SpooferConfig.getDefaultScope().SPOOF_NEW_PLAYERS_KEEPSKIN, // default value
                                    () -> opts.SPOOF_NEW_PLAYERS_KEEPSKIN,
                                    value -> opts.SPOOF_NEW_PLAYERS_KEEPSKIN = value
                            )
                            .controller(TickBoxControllerBuilder::create)
                            .build()
                    ).build());
        }

        return groups;
    }
}
