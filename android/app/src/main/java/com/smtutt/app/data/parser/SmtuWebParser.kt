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

        // 1. Live layout on smtu.ru: <h3>Faculty Name</h3> followed by group links in subsequent elements
        val h3Elements = doc.select("h3")
        for (h3 in h3Elements) {
            val facName = h3.text().trim()
            if (facName.isBlank()) continue

            val groups = mutableListOf<GroupInfo>()
            val seenIds = mutableSetOf<String>()

            var sibling = h3.nextElementSibling()
            while (sibling != null && !sibling.tagName().equals("h3", ignoreCase = true)) {
                val links = sibling.select("a[href*=/viewschedule_new/], a[href*=/viewschedule/]")
                for (a in links) {
                    val href = a.attr("href")
                    val id = href.trim('/').split('/').lastOrNull() ?: ""
                    val name = a.text().trim()
                    if (id.isNotBlank() && name.isNotBlank() && seenIds.add(id)) {
                        groups.add(
                            GroupInfo(
                                id = id,
                                name = name,
                                url = href
                            )
                        )
                    }
                }
                sibling = sibling.nextElementSibling()
            }

            if (groups.isNotEmpty()) {
                facultiesData.add(
                    FacultyInfo(
                        faculty = facName,
                        courses = listOf(
                            CourseInfo(
                                course = "Группы",
                                groups = groups
                            )
                        )
                    )
                )
            }
        }

        // 2. Fallback: section.schedule-group-section
        if (facultiesData.isEmpty()) {
            val sections = doc.select("section.schedule-group-section, section")
            for (sec in sections) {
                val h2 = sec.select("h2, h3").firstOrNull()
                val facName = h2?.text()?.trim() ?: "Без названия"

                val groupLinks = sec.select("a.gr-link, a[href*=/ru/viewschedule_new/], a[href*=/viewschedule/]")
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
                    facultiesData.add(
                        FacultyInfo(
                            faculty = facName,
                            courses = listOf(
                                CourseInfo(
                                    course = "Группы",
                                    groups = groups
                                )
                            )
                        )
                    )
                }
            }
        }

        return facultiesData
    }

    fun extractSearchKey(htmlContent: String): String? {
        val doc: Document = Jsoup.parse(htmlContent)
        val input = doc.select("form[action*=/ru/searchschedule/] input[name=search_key], input[name=search_key]").firstOrNull()
        return input?.attr("value")?.trim()
    }

    fun parseTeacherResults(htmlContent: String): List<TeacherSearchResult> {
        val doc: Document = Jsoup.parse(htmlContent)
        val results = mutableListOf<TeacherSearchResult>()
        val seenIds = mutableSetOf<String>()

        val links = doc.select("ul.schedule-search-results a, a[href*=/viewschedule_new/teacher/], a[href*=/viewschedule/teacher/], a[href*=/teacher/]")
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

        // 2. Day blocks: cards with tables inside #table-container or directly in doc
        var dayBlocks = doc.select("#table-container .card")
        if (dayBlocks.isEmpty()) {
            dayBlocks = doc.select(".card:has(table)")
        }
        if (dayBlocks.isEmpty()) {
            dayBlocks = doc.select("#table-container .js-day-block, .js-day-block")
        }
        if (dayBlocks.isEmpty()) {
            dayBlocks = doc.select("table:has(tr)")
        }

        val daysList = mutableListOf<DaySchedule>()

        for (block in dayBlocks) {
            val h = block.select("h2, h3, h4, .card-header").firstOrNull()
            var dayName = h?.text()?.trim() ?: "Учебный день"

            val words = dayName.split("\\s+".toRegex())
            if (words.size >= 2 && words[0].equals(words[1], ignoreCase = true)) {
                dayName = words[0]
            }

            val table = if (block.tagName().equals("table", ignoreCase = true)) block else block.select("table").firstOrNull() ?: continue
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

                // td[0]: Week type (icon / text / tooltip / class)
                val weekIcon = tds[0].select("i, span").firstOrNull()
                var weekType = weekIcon?.attr("data-bs-title")?.trim() ?: ""
                if (weekType.isBlank()) weekType = weekIcon?.attr("title")?.trim() ?: ""
                if (weekType.isBlank()) {
                    val tdHtml = tds[0].outerHtml()
                    val tdClass = tds[0].className()
                    val trId = tr.id()
                    weekType = when {
                        tdHtml.contains("fa-arrow-up") || trId.contains("week-up") || tdClass.contains("text-success") -> "Верхняя неделя"
                        tdHtml.contains("fa-arrow-down") || trId.contains("week-down") || tdClass.contains("text-warning") -> "Нижняя неделя"
                        tdHtml.contains("fa-repeat") || trId.contains("week-both") || tdClass.contains("text-info") -> "Обе недели"
                        else -> tds[0].text().trim()
                    }
                }

                // Columns:
                // th=time, td[0]=week, td[1]=classroom, td[2]=group, td[3]=subject, td[4]=teacher
                val classroom: String
                val group: String
                val subjectCell: Element?
                val teacherTd: Element?

                if (tds.size >= 5) {
                    classroom = tds[1].text().trim()
                    group = tds[2].text().trim()
                    subjectCell = tds[3]
                    teacherTd = tds[4]
                } else if (tds.size == 4) {
                    classroom = tds[1].text().trim()
                    group = if (isTeacher) tds[2].text().trim() else ""
                    subjectCell = if (isTeacher) tds[3] else tds[2]
                    teacherTd = if (isTeacher) null else tds[3]
                } else {
                    classroom = if (tds.size > 1) tds[1].text().trim() else ""
                    group = ""
                    subjectCell = if (tds.size > 2) tds[2] else null
                    teacherTd = if (tds.size > 3) tds[3] else null
                }

                val subjectInfo = if (subjectCell != null) parseSubjectCell(subjectCell) else SubjectParseResult("", null, null, "")

                var teacherName = ""
                var teacherId = ""
                if (teacherTd != null) {
                    teacherName = teacherTd.text().trim()
                    val tLink = teacherTd.select("a").firstOrNull()
                    if (tLink != null) {
                        val href = tLink.attr("href")
                        teacherId = href.trim('/').split('/').lastOrNull() ?: ""
                    }
                }

                lessons.add(
                    Lesson(
                        time = timeStr,
                        weekType = weekType,
                        dates = subjectInfo.dates,
                        classroom = classroom,
                        group = group,
                        subject = subjectInfo.subject,
                        lessonType = subjectInfo.lessonType,
                        subgroup = subjectInfo.subgroup,
                        teacherName = teacherName,
                        teacherId = teacherId
                    )
                )
            }

            if (lessons.isNotEmpty() || dayBlocks.size <= 7) {
                daysList.add(
                    DaySchedule(
                        dayName = dayName,
                        lessons = lessons
                    )
                )
            }
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

    data class SubjectParseResult(
        val subject: String,
        val lessonType: String?,
        val subgroup: String?,
        val dates: String
    )

    private fun parseSubjectCell(td: Element): SubjectParseResult {
        val spanEl = td.select("span").firstOrNull()
        var rawSubject = spanEl?.text()?.trim() ?: ""

        var lessonType: String? = null
        var subgroup: String? = null
        var dates = ""

        val smallElements = td.select("small")
        for (sm in smallElements) {
            val text = sm.text().trim()
            if (text.isBlank()) continue
            if (text.contains("п/г", ignoreCase = true) || text.contains("подгруппа", ignoreCase = true)) {
                subgroup = text
            } else if (lessonTypeRegex.containsMatchIn(text) || sm.className().contains("text-muted")) {
                if (lessonType == null) {
                    lessonType = text
                }
            } else if (Regex("\\d{2}\\.\\d{2}").containsMatchIn(text)) {
                dates = text
            } else if (rawSubject.isBlank()) {
                rawSubject = text
            }
        }

        if (rawSubject.isBlank()) {
            val clone = td.clone()
            clone.select("small").remove()
            rawSubject = clone.text().trim()
        }

        if (rawSubject.isBlank()) {
            rawSubject = td.text().trim()
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

        return SubjectParseResult(cleanSubj, lessonType, subgroup, dates)
    }
}
