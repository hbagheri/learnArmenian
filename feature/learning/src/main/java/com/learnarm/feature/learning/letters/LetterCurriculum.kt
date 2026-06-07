package com.learnarm.feature.learning.letters

import com.learnarm.core.data.api.BatchWordDto
import com.learnarm.core.data.api.LetterBatchDto
import com.learnarm.core.data.api.LetterCurriculumDto

/**
 * Offline fallback for the Level-1 letter-batch curriculum, used only when the
 * `/api/letter-curriculum` endpoint is unreachable (first launch offline, network
 * blip). The authoritative version lives on the backend — see
 * `~/projects/personal/localWebHosting/fastapi/learnArm/app/curriculum.py`.
 * Keep this in sync only when the canonical ordering changes; do not edit it
 * to add letters or words without updating the server too.
 *
 * Letter ids reference [com.learnarm.core.database.entity.LetterEntity].
 */
val FALLBACK_LETTER_CURRICULUM: LetterCurriculumDto = LetterCurriculumDto(
    periodicReviewBatches = listOf(4, 8),
    passThresholdPercent = 100,
    hintTimeoutMs = 8_000L,
    batches = listOf(
        LetterBatchDto(
            index = 1, round = 1,
            letterIds = listOf(1, 20, 22, 11), // Ա, Մ, Ն, Ի
            targetWord = BatchWordDto("մամա", "mama", "ماما"),
            readableWords = listOf(
                BatchWordDto("մամա", "mama", "ماما"),
                BatchWordDto("նա", "na", "او"),
                BatchWordDto("մի", "mi", "یک / نکن"),
                BatchWordDto("ինա", "ina", "Ina (نام)"),
                BatchWordDto("ման", "man", "جستجو"),
            ),
        ),
        LetterBatchDto(
            index = 2, round = 1,
            letterIds = listOf(5, 32, 21, 29), // Ե, Ր, Յ, Ս
            targetWord = BatchWordDto("մայր", "mayr", "مادر"),
            readableWords = listOf(
                BatchWordDto("մայր", "mayr", "مادر"),
                BatchWordDto("ես", "yes", "من"),
                BatchWordDto("սեր", "ser", "عشق"),
                BatchWordDto("սա", "sa", "این"),
                BatchWordDto("յար", "yar", "یار"),
                BatchWordDto("մամա", "mama", "ماما"),
                BatchWordDto("նա", "na", "او"),
            ),
        ),
        LetterBatchDto(
            index = 3, round = 1,
            letterIds = listOf(16, 39, 31, 24), // Հ, և, Տ, Ո
            targetWord = BatchWordDto("արև", "arev", "خورشید"),
            readableWords = listOf(
                BatchWordDto("արև", "arev", "خورشید"),
                BatchWordDto("հայր", "hayr", "پدر"),
                BatchWordDto("հայ", "hay", "ارمنی"),
                BatchWordDto("հիմա", "hima", "الآن"),
                BatchWordDto("տես", "tes", "ببین"),
                BatchWordDto("սիրտ", "sirt", "قلب"),
                BatchWordDto("հա", "ha", "آره"),
                BatchWordDto("մայր", "mayr", "مادر"),
                BatchWordDto("սեր", "ser", "عشق"),
                BatchWordDto("ես", "yes", "من"),
            ),
        ),
        LetterBatchDto(
            index = 4, round = 1,
            letterIds = listOf(34, 2, 15, 12), // Ու, Բ, Կ, Լ
            targetWord = BatchWordDto("բարև", "barev", "سلام"),
            readableWords = listOf(
                BatchWordDto("բարև", "barev", "سلام"),
                BatchWordDto("տուն", "tun", "خانه"),
                BatchWordDto("բարի", "bari", "خوب / مهربان"),
                BatchWordDto("կամ", "kam", "هستم"),
                BatchWordDto("լույս", "luys", "نور"),
                BatchWordDto("կարմիր", "karmir", "قرمز"),
                BatchWordDto("մատ", "mat", "انگشت"),
                BatchWordDto("արև", "arev", "خورشید"),
                BatchWordDto("մայր", "mayr", "مادر"),
                BatchWordDto("հայր", "hayr", "پدر"),
            ),
        ),
        LetterBatchDto(
            index = 5, round = 2,
            letterIds = listOf(26, 23, 25, 30), // Պ, Շ, Չ, Վ
            targetWord = BatchWordDto("չորս", "chors", "چهار"),
            readableWords = listOf(
                BatchWordDto("չորս", "chors", "چهار"),
                BatchWordDto("շուն", "shun", "سگ"),
                BatchWordDto("վարդ", "vard", "گل سرخ"),
                BatchWordDto("պապա", "papa", "بابا"),
                BatchWordDto("շատ", "shat", "خیلی"),
                BatchWordDto("վար", "var", "راندن"),
                BatchWordDto("պատ", "pat", "دیوار"),
                BatchWordDto("ապա", "apa", "پس"),
                BatchWordDto("բարև", "barev", "سلام"),
                BatchWordDto("տուն", "tun", "خانه"),
            ),
        ),
        LetterBatchDto(
            index = 6, round = 2,
            letterIds = listOf(4, 9, 18, 17), // Դ, Թ, Ղ, Ձ
            targetWord = BatchWordDto("դուռ", "dur", "در"),
            readableWords = listOf(
                BatchWordDto("դուռ", "dur", "در"),
                BatchWordDto("ձուկ", "dzuk", "ماهی"),
                BatchWordDto("դաս", "das", "درس"),
                BatchWordDto("դեմ", "dem", "مقابل"),
                BatchWordDto("թաթ", "tat", "پنجه"),
                BatchWordDto("թագ", "tag", "تاج"),
                BatchWordDto("ղեկ", "ghek", "سکّان"),
                BatchWordDto("մարդ", "mard", "انسان / مرد"),
                BatchWordDto("բարև", "barev", "سلام"),
                BatchWordDto("տուն", "tun", "خانه"),
            ),
        ),
        LetterBatchDto(
            index = 7, round = 2,
            letterIds = listOf(33, 27, 13, 28), // Ց, Ջ, Խ, Ռ
            targetWord = BatchWordDto("ջուր", "jur", "آب"),
            readableWords = listOf(
                BatchWordDto("ջուր", "jur", "آب"),
                BatchWordDto("ցավ", "tsav", "درد"),
                BatchWordDto("ցուրտ", "tsurt", "سرد"),
                BatchWordDto("խաղ", "khagh", "بازی"),
                BatchWordDto("ռադիո", "radio", "رادیو"),
                BatchWordDto("դուռ", "dur", "در"),
                BatchWordDto("բարև", "barev", "سلام"),
            ),
        ),
        LetterBatchDto(
            index = 8, round = 2,
            letterIds = listOf(7, 8, 36, 37), // Է, Ը, Ք, Օ
            targetWord = BatchWordDto("է", "e", "هست (فعل ربطی)"),
            readableWords = listOf(
                BatchWordDto("է", "e", "هست"),
                BatchWordDto("օր", "or", "روز"),
                BatchWordDto("ընկեր", "ynker", "دوست"),
                BatchWordDto("քամի", "kami", "باد"),
                BatchWordDto("քեռի", "keri", "دایی"),
                BatchWordDto("քաղաք", "kaghak", "شهر"),
                BatchWordDto("էջ", "ej", "صفحه"),
                BatchWordDto("խոսք", "khosk", "سخن"),
                BatchWordDto("բարև", "barev", "سلام"),
                BatchWordDto("ջուր", "jur", "آب"),
            ),
        ),
        LetterBatchDto(
            index = 9, round = 3,
            letterIds = listOf(3, 10, 14, 19), // Գ, Ժ, Ծ, Ճ
            targetWord = BatchWordDto("գիրք", "girk", "کتاب"),
            readableWords = listOf(
                BatchWordDto("գիրք", "girk", "کتاب"),
                BatchWordDto("ծառ", "tsar", "درخت"),
                BatchWordDto("ճանապարհ", "chanaparh", "راه"),
                BatchWordDto("ժամ", "zham", "ساعت / زمان"),
                BatchWordDto("գարուն", "garun", "بهار"),
                BatchWordDto("ծով", "tsov", "دریا"),
                BatchWordDto("բարև", "barev", "سلام"),
                BatchWordDto("ընկեր", "ynker", "دوست"),
            ),
        ),
        LetterBatchDto(
            index = 10, round = 3,
            letterIds = listOf(6, 35, 38), // Զ, Փ, Ֆ
            targetWord = BatchWordDto("փող", "pogh", "پول"),
            readableWords = listOf(
                BatchWordDto("փող", "pogh", "پول"),
                BatchWordDto("ֆիլմ", "film", "فیلم"),
                BatchWordDto("զատիկ", "zatik", "کفش‌دوزک"),
                BatchWordDto("բարև", "barev", "سلام"),
                BatchWordDto("տուն", "tun", "خانه"),
                BatchWordDto("ջուր", "jur", "آب"),
                BatchWordDto("ընկեր", "ynker", "دوست"),
            ),
        ),
    ),
)

/** Letter ids unlocked after completing batches 1..batchIndex (inclusive). */
fun LetterCurriculumDto.cumulativeLetterIds(throughBatchIndex: Int): List<Int> =
    batches.filter { it.index <= throughBatchIndex }.flatMap { it.letterIds }
