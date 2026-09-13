import re
from typing import List, Tuple, Optional
import httpx
from lxml import html

from app.models import (
    FacultyInfo, CourseInfo, GroupInfo,
    TeacherSearchResult, Lesson, DaySchedule, ScheduleResponse
)

BASE_URL = "https://www.smtu.ru"
HEADERS = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
    "Accept-Language": "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7",
}

def clean_text(text: Optional[str]) -> str:
    if not text:
        return ""
    return re.sub(r'\s+', ' ', text).strip()

def parse_subject_cell(td) -> Tuple[str, Optional[str], Optional[str]]:
    full_text = td.text_content().strip()
    lines = [line.strip() for line in full_text.split('\n') if line.strip()]
    
    lesson_type = None
    subgroup = None
    subject_parts = []
    
    known_types = [
        'Лекция', 'Практическое занятие', 'Лабораторное занятие',
        'Консультация', 'Зачет', 'Дифференцированный зачет', 'Экзамен',
        'Курсовое проектирование', 'Курсовая работа'
    ]
    
    for line in lines:
        matched_type = False
        for kt in known_types:
            if kt.lower() in line.lower():
                lesson_type = kt
                matched_type = True
                break
        if matched_type:
            continue
            
        if 'п/г' in line.lower() or 'подгруппа' in line.lower():
            subgroup = line
            continue
            
        subject_parts.append(line)
        
    subject = " ".join(subject_parts).strip()
    return subject, lesson_type, subgroup

async def get_faculties_and_groups() -> List[FacultyInfo]:
    url = f"{BASE_URL}/ru/listschedule/"
    async with httpx.AsyncClient(headers=HEADERS, verify=False, timeout=20.0) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        tree = html.fromstring(resp.text)
        
    faculties_data: List[FacultyInfo] = []
    sections = tree.xpath("//section[contains(@class, 'schedule-group-section')]")
    
    for sec in sections:
        h2 = sec.xpath(".//h2 | .//h3")
        fac_name = clean_text(h2[0].text_content()) if h2 else "Без названия"
        
        courses_data: List[CourseInfo] = []
        course_cols = sec.xpath(".//*[contains(@class, 'schedule-course-column') or contains(@class, 'schedule-course-grid') or contains(@class, 'schedule-course-col')]")
        if not course_cols:
            course_cols = [sec]
            
        for col in course_cols:
            course_headers = col.xpath(".//*[contains(@class, 'schedule-course-title') or self::h4 or self::h5]")
            course_name = clean_text(course_headers[0].text_content()) if course_headers else "Курс"
            
            group_links = col.xpath(".//a[contains(@class, 'gr-link') or contains(@href, '/ru/viewschedule_new/')]")
            groups: List[GroupInfo] = []
            seen_ids = set()
            for g in group_links:
                g_name = clean_text(g.text_content())
                g_href = g.get('href', '')
                g_id = g_href.strip('/').split('/')[-1]
                if g_id and g_id not in seen_ids:
                    seen_ids.add(g_id)
                    groups.append(GroupInfo(
                        id=g_id,
                        name=g_name,
                        url=g_href
                    ))
            if groups:
                courses_data.append(CourseInfo(
                    course=course_name,
                    groups=groups
                ))
                
        faculties_data.append(FacultyInfo(
            faculty=fac_name,
            courses=courses_data
        ))
        
    return faculties_data

async def search_teachers(surname: str) -> List[TeacherSearchResult]:
    list_url = f"{BASE_URL}/ru/listschedule/"
    search_url = f"{BASE_URL}/ru/searchschedule/"
    
    # We must maintain cookies (PHPSESSID) across listschedule and searchschedule
    async with httpx.AsyncClient(headers=HEADERS, verify=False, timeout=20.0, follow_redirects=True) as client:
        r_list = await client.get(list_url)
        r_list.raise_for_status()
        
        tree_list = html.fromstring(r_list.text)
        search_key_el = tree_list.xpath("//form[@action='/ru/searchschedule/']//input[@name='search_key']")
        if not search_key_el:
            return []
        search_key = search_key_el[0].get('value', '')
        
        post_headers = {
            "Referer": list_url,
            "Content-Type": "application/x-www-form-urlencoded",
        }
        data = {
            "search_key": search_key,
            "whatsearch": surname.strip()
        }
        
        r_search = await client.post(search_url, data=data, headers=post_headers)
        r_search.raise_for_status()
        
        tree_search = html.fromstring(r_search.text)
        
        results: List[TeacherSearchResult] = []
        teacher_links = tree_search.xpath("//ul[contains(@class, 'schedule-search-results')]//a | //a[contains(@href, '/ru/viewschedule_new/teacher/')]")
        
        seen_ids = set()
        for a in teacher_links:
            t_name = clean_text(a.text_content())
            href = a.get('href', '')
            t_id = href.strip('/').split('/')[-1]
            if t_id and t_id not in seen_ids:
                seen_ids.add(t_id)
                results.append(TeacherSearchResult(
                    id=t_id,
                    name=t_name,
                    url=href
                ))
                
        return results

def _parse_schedule_html(html_content: str, entity_id: str, is_teacher: bool) -> ScheduleResponse:
    tree = html.fromstring(html_content)
    
    # Extract page title / entity name
    extracted_title = ""
    # Look for h1 with "Расписание занятий"
    h1_sched = tree.xpath("//h1[contains(text(), 'Расписание занятий')]")
    if h1_sched:
        extracted_title = clean_text(h1_sched[0].text_content())
    else:
        title_candidates = tree.xpath("//h1 | //h2")
        for c in title_candidates:
            t = clean_text(c.text_content())
            if t and len(t) < 80 and not any(skip in t.lower() for skip in ['спбгмту', 'навигация']):
                extracted_title = t
                break
    if not extracted_title:
        extracted_title = f"{'Преподаватель' if is_teacher else 'Группа'} {entity_id}"
        
    day_blocks = tree.xpath("//div[@id='table-container']//div[contains(@class, 'js-day-block')]")
    if not day_blocks:
        day_blocks = tree.xpath("//div[contains(@class, 'js-day-block')]")
        
    days_list: List[DaySchedule] = []
    
    for block in day_blocks:
        h = block.xpath(".//*[self::h2 or self::h3 or self::h4 or contains(@class, 'card-header')]")
        day_name = clean_text(h[0].text_content()) if h else "Учебный день"
        
        # In case title repeats, e.g. "Понедельник Понедельник"
        words = day_name.split()
        if len(words) >= 2 and words[0] == words[1]:
            day_name = words[0]
            
        tbl = block.xpath(".//table")
        if not tbl:
            continue
            
        table = tbl[0]
        rows = table.xpath(".//tr")
        lessons: List[Lesson] = []
        
        for tr in rows:
            # Skip pure header row
            if tr.xpath("./th") and not tr.xpath("./td"):
                continue
                
            time_el = tr.xpath("./th")
            time_str = clean_text(time_el[0].text_content()) if time_el else ""
            
            tds = tr.xpath("./td")
            if not tds:
                continue
                
            week_type = clean_text(tds[0].text_content()) if len(tds) > 0 else ""
            dates = clean_text(tds[1].text_content()) if len(tds) > 1 else ""
            classroom = clean_text(tds[2].text_content()) if len(tds) > 2 else ""
            group = clean_text(tds[3].text_content()) if len(tds) > 3 else ""
            
            subject, lesson_type, subgroup = parse_subject_cell(tds[4]) if len(tds) > 4 else ("", None, None)
            
            teacher_name = ""
            teacher_id = ""
            if len(tds) > 5:
                teacher_td = tds[5]
                teacher_name = clean_text(teacher_td.text_content())
                t_links = teacher_td.xpath(".//a")
                if t_links:
                    href = t_links[0].get('href', '')
                    teacher_id = href.strip('/').split('/')[-1]
                    
            lessons.append(Lesson(
                time=time_str,
                week_type=week_type,
                dates=dates,
                classroom=classroom,
                group=group,
                subject=subject,
                lesson_type=lesson_type,
                subgroup=subgroup,
                teacher_name=teacher_name,
                teacher_id=teacher_id
            ))
            
        days_list.append(DaySchedule(
            day_name=day_name,
            lessons=lessons
        ))
        
    # If teacher schedule and title is generic, find teacher name from lessons
    if is_teacher and ("преподаватель" in extracted_title.lower() or not extracted_title):
        for d in days_list:
            for l in d.lessons:
                if l.teacher_name:
                    extracted_title = f"Расписание: {l.teacher_name}"
                    break
            if "преподаватель" not in extracted_title.lower():
                break

    return ScheduleResponse(
        id=entity_id,
        title=extracted_title,
        is_teacher=is_teacher,
        days=days_list
    )

async def parse_group_schedule(group_id: str) -> ScheduleResponse:
    url = f"{BASE_URL}/ru/viewschedule_new/{group_id}/"
    async with httpx.AsyncClient(headers=HEADERS, verify=False, timeout=20.0) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        return _parse_schedule_html(resp.text, entity_id=group_id, is_teacher=False)

async def parse_teacher_schedule(teacher_id: str) -> ScheduleResponse:
    url = f"{BASE_URL}/ru/viewschedule_new/teacher/{teacher_id}/"
    async with httpx.AsyncClient(headers=HEADERS, verify=False, timeout=20.0) as client:
        resp = await client.get(url)
        resp.raise_for_status()
        return _parse_schedule_html(resp.text, entity_id=teacher_id, is_teacher=True)
