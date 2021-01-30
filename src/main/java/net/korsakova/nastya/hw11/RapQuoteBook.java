package net.korsakova.nastya.hw11;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RapQuoteBook {
    JIM_RON("Если вы не готовы рискнуть обычным, вам придется довольствоваться заурядным"),
    HOWARD_TRUEMAN("Не спрашивайте, в чем нуждается мир, — спросите себя, что наполняет вас жизнью. Миру нужны люди, наполненные жизнью"),
    LOO_HOLTC("Не ноша тянет вас вниз, а то, как вы ее несете"),
    KRIS_GROSSER("Возможности не приходят сами — вы создаете их"),
    AUDREY_HAPBURN("Нет ничего невозможного. Само слово говорит: „Я возможно!“ (Impossible — I'm possible)")
    ;

    private final String quote;
}
