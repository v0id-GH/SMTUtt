package com.smtutt.app.data.parser

import com.smtutt.app.data.model.*
import com.smtutt.app.ui.viewmodel.WeekFilter
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

object SmtuWebParser {

    fun parseCurrentWeekFromListSchedule(htmlContent: String): WeekFilter? {
        val doc: Document = Jsoup.parse(htmlContent)
        val todayEl = doc.select(".schedule-today, h4:contains(Сегодня)").firstOrNull() ?: return null
        val text = todayEl.text().lowercase()
        return when {
            text.contains("верхняя") -> WeekFilter.UPPER
            text.contains("нижняя") -> WeekFilter.LOWER
            else -> null
        }
    }

    private val knownTypes = listOf(
        "Лекция", "Практическое занятие", "Лабораторное занятие",
        "Консультация", "Зачет", "Дифференцированный зачет", "Экзамен",
        "Курсовое проектирование", "Курсовая работа"
    )

    fun parseFacultiesAndGroups(htmlContent: String): List<FacultyInfo> {
        val doc: Document = Jsoup.parse(htmlContent)
        val facultiesData = mutableListOf<FacultyInfo>()

        val sections = doc.select("section.schedule-group-section")
        for (sec in sections) {
            val h2 = sec.select("h2, h3").firstOrNull()
            val facName = h2?.text()?.trim() ?: "Без названия"

            val coursesData = mutableListOf<CourseInfo>()
            var courseCols = sec.select(".schedule-course-column, .schedule-course-grid, .schedule-course-col")
            if (courseCols.isEmpty()) {
                courseCols = org.jsoup.select.Elements(sec)
            }

            for (col in courseCols) {
                val courseHeader = col.select(".schedule-course-title, h4, h5").firstOrNull()
                val courseName = courseHeader?.text()?.trim() ?: "Курс"

                val groupLinks = col.select("a.gr-link, a[href*=/ru/viewschedule_new/]")
                val groups = mutableListOf<GroupInfo>()
                val seenIds = mutableSetOf<String>()

                for (g in groupLinks) {
                    val gName = g.text().trim()
                    val gHref = g.attr("href")
                    val gId = gHref.trim('/').split('/').lastOrNull() ?: ""

                    if (gId.isNotBlank() && seenIds.add(gId)) {
                        groups.add(
                            GroupInfo(
                                id = gId,
                                name = gName,
                                url = gHref
                            )
                        )
                    }
                }

                if (groups.isNotEmpty()) {
                    coursesData.add(
                        CourseInfo(
                            course = courseName,
                            groups = groups
                        )
                    )
                }
            }

            facultiesData.add(
                FacultyInfo(
                    faculty = facName,
                    courses = coursesData
                )
            )
        }

        return facultiesData
    }

    fun extractSearchKey(htmlContent: String): String? {
        val doc: Document = Jsoup.parse(htmlContent)
        val input = doc.select("form[action*=/ru/searchschedule/] input[name=search_key]").firstOrNull()
        return input?.attr("value")?.trim()
    }

    fun parseTeacherResults(htmlContent: String): List<TeacherSearchResult> {
        val doc: Document = Jsoup.parse(htmlContent)
        val results = mutableListOf<TeacherSearchResult>()
        val seenIds = mutableSetOf<String>()

        val links = doc.select("ul.schedule-search-results a, a[href*=/ru/viewschedule_new/teacher/]")
        for (a in links) {
            val tName = a.text().trim()
            val href = a.attr("href")
            val tId = href.trim('/').split('/').lastOrNull() ?: ""

            if (tId.isNotBlank() && seenIds.add(tId)) {
                results.add(
                    TeacherSearchResult(
                        id = tId,
                        name = tName,
                        url = href
                    )
                )
            }
        }

        return results
    }

    fun parseScheduleHtml(htmlContent: String, entityId: String, isTeacher: Boolean): ScheduleResponse {
        val doc: Document = Jsoup.parse(htmlContent)

        // 1. Title extraction
        var extractedTitle = ""
        val h1Sched = doc.select("h1:contains(Расписание занятий)").firstOrNull()
        if (h1Sched != null) {
            extractedTitle = h1Sched.text().trim()
        } else {
            val candidates = doc.select("h1, h2")
            for (c in candidates) {
                val t = c.text().trim()
                if (t.isNotBlank() && t.length < 80 && !t.contains("СПбГМТУ", ignoreCase = true) && !t.contains("Навигация", ignoreCase = true)) {
                    extractedTitle = t
                    break
                }
            }
        }

        if (extractedTitle.isBlank()) {
            extractedTitle = if (isTeacher) "Преподаватель $entityId" else "Группа $entityId"
        }

        // 2. Day blocks
        var dayBlocks = doc.select("#table-container .js-day-block")
        if (dayBlocks.isEmpty()) {
            dayBlocks = doc.select(".js-day-block")
        }

        val daysList = mutableListOf<DaySchedule>()

        for (block in dayBlocks) {
            val h = block.select("h2, h3, h4, .card-header").firstOrNull()
            var dayName = h?.text()?.trim() ?: "Учебный день"

            val words = dayName.split("\\s+".toRegex())
            if (words.size >= 2 && words[0].equals(words[1], ignoreCase = true)) {
                dayName = words[0]
            }

            val table = block.select("table").firstOrNull() ?: continue
            val rows = table.select("tr")
            val lessons = mutableListOf<Lesson>()

            for (tr in rows) {
                // Skip table header
                if (tr.select("th").isNotEmpty() && tr.select("td").isEmpty()) {
                    continue
                }

                val timeEl = tr.select("th").firstOrNull()
                val timeStr = timeEl?.text()?.trim() ?: ""

                val tds = tr.select("td")
                if (tds.isEmpty()) continue

                val weekType = if (tds.size > 0) tds[0].text().trim() else ""
                val dates = if (tds.size > 1) tds[1].text().trim() else ""
                val classroom = if (tds.size > 2) tds[2].text().trim() else ""
                val group = if (tds.size > 3) tds[3].text().trim() else ""

                val (subject, lessonType, subgroup) = if (tds.size > 4) parseSubjectCell(tds[4]) else Triple("", null, null)

                var teacherName = ""
                var teacherId = ""
                if (tds.size > 5) {
                    val teacherTd = tds[5]
                    teacherName = teacherTd.text().trim()
                    val tLinks = teacherTd.select("a")
                    if (tLinks.isNotEmpty()) {
                        val href = tLinks.first()?.attr("href") ?: ""
                        teacherId = href.trim('/').split('/').lastOrNull() ?: ""
                    }
                }

                lessons.add(
                    Lesson(
                        time = timeStr,
                        weekType = weekType,
                        dates = dates,
                        classroom = classroom,
                        group = group,
                        subject = subject,
                        lessonType = lessonType,
                        subgroup = subgroup,
                        teacherName = teacherName,
                        teacherId = teacherId
                    )
                )
            }

            daysList.add(
                DaySchedule(
                    dayName = dayName,
                    lessons = lessons
                )
            )
        }

        // If teacher schedule and title is generic, find teacher name from lessons
        if (isTeacher && (extractedTitle.contains("преподаватель", ignoreCase = true) || extractedTitle.isBlank())) {
            for (d in daysList) {
                val found = d.lessons.firstOrNull { it.teacherName.isNotBlank() }
                if (found != null) {
                    extractedTitle = "Расписание: ${found.teacherName}"
                    break
                }
            }
        }

        return ScheduleResponse(
            id = entityId,
            title = extractedTitle,
            isTeacher = isTeacher,
            days = daysList
        )
    }

    val lessonTypeRegex = Regex(
        "(?i)\\b(практическ(?:ое|ие|ая)\\s+(?:заняти[ея]|задани[ея]|работ[аы])|" +
        "лабораторн(?:ое|ые|ая)\\s+(?:заняти[ея]|работ[аы])|" +
        "лекционн(?:ое|ые)\\s+заняти[ея]|" +
        "дифференцированный\\s+зач[её]т|диф\\.?\\s*зач[её]т|" +
        "курсов(?:ое|ая|ой)\\s+(?:проектирование|работ[аы]|проект)|" +
        "лекци[яи]|практик[аи]|консультаци[яи]|зач[её]т(?:ы)?|экзамен(?:ы)?|семинар(?:ы)?)\\b"
    )

    private val subgroupRegex = Regex("(?i)[\\(\\[]?(\\d+[-ея]?\\s*п/?г|\\d+\\s*подгруппа)[\\)\\]]?")

    fun cleanSubject(rawSubject: String): String {
        return rawSubject
            .replace(lessonTypeRegex, "")
            .replace(subgroupRegex, "")
            .replace(Regex("[\\(\\)\\[\\]\\s]+$"), "")
            .replace(Regex("^[\\(\\)\\[\\]\\s]+"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun extractLessonType(rawSubject: String): String? {
        val match = lessonTypeRegex.find(rawSubject) ?: return null
        val matchedStr = match.value.trim()
        return matchedStr.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun extractSubgroup(rawSubject: String): String? {
        val match = subgroupRegex.find(rawSubject) ?: return null
        return match.value.trim('(', ')', '[', ']', ' ')
    }

    private fun parseSubjectCell(td: Element): Triple<String, String?, String?> {
        val spanEl = td.select("span").firstOrNull()
        var rawSubject = spanEl?.text()?.trim() ?: ""

        var lessonType: String? = null
        var subgroup: String? = null

        val smallElements = td.select("small")
        for (sm in smallElements) {
            val text = sm.text().trim()
            if (text.isBlank()) continue
            if (text.contains("п/г", ignoreCase = true) || text.contains("подгруппа", ignoreCase = true)) {
                subgroup = text
            } else if (lessonType == null) {
                lessonType = text
            }
        }

        if (rawSubject.isBlank()) {
            val clone = td.clone()
            clone.select("small").remove()
            rawSubject = clone.text().trim()
        }

        if (lessonType == null) {
            lessonType = extractLessonType(rawSubject)
        }

        if (subgroup == null) {
            subgroup = extractSubgroup(rawSubject)
        }

        var cleanSubj = cleanSubject(rawSubject)
        if (cleanSubj.isBlank()) {
            cleanSubj = td.text().trim()
        }

        return Triple(cleanSubj, lessonType, subgroup)
    }
}
