from typing import List, Optional
from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware

from app.models import (
    FacultyInfo, GroupInfo, TeacherSearchResult, ScheduleResponse
)
from app.parser import (
    get_faculties_and_groups, search_teachers,
    parse_group_schedule, parse_teacher_schedule
)
from app.cache import cache

app = FastAPI(
    title="SMTU Schedule API",
    description="REST API для получения и парсинга расписания Санкт-Петербургского государственного морского технического университета (СПбГМТУ)",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get("/")
def read_root():
    return {
        "name": "SMTU Schedule API",
        "status": "online",
        "docs": "/docs",
        "endpoints": [
            "/api/faculties",
            "/api/groups",
            "/api/schedule/group/{group_id}",
            "/api/teachers/search?q={query}",
            "/api/schedule/teacher/{teacher_id}"
        ]
    }

@app.get("/api/faculties", response_model=List[FacultyInfo])
async def get_faculties(refresh: bool = False):
    cache_key = "faculties_list"
    if not refresh:
        cached = cache.get(cache_key)
        if cached:
            return cached
            
    try:
        data = await get_faculties_and_groups()
        cache.set(cache_key, data, ttl=3600)  # 1 hour
        return data
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Ошибка парсинга списка факультетов: {str(e)}")

@app.get("/api/groups", response_model=List[GroupInfo])
async def get_groups(
    search: Optional[str] = Query(None, description="Поиск по номеру группы"),
    faculty: Optional[str] = Query(None, description="Фильтр по названию факультета")
):
    faculties = await get_faculties()
    results: List[GroupInfo] = []
    seen = set()
    
    search_clean = search.strip().lower() if search else None
    
    for f in faculties:
        if faculty and faculty.lower() not in f.faculty.lower():
            continue
        for c in f.courses:
            for g in c.groups:
                if g.id not in seen:
                    if search_clean and search_clean not in g.name.lower():
                        continue
                    seen.add(g.id)
                    results.append(g)
                    
    return results

@app.get("/api/schedule/group/{group_id}", response_model=ScheduleResponse)
async def get_group_schedule(group_id: str, refresh: bool = False):
    cache_key = f"schedule_group_{group_id}"
    if not refresh:
        cached = cache.get(cache_key)
        if cached:
            return cached
            
    try:
        sched = await parse_group_schedule(group_id)
        cache.set(cache_key, sched, ttl=1800)  # 30 mins
        return sched
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Ошибка получения расписания группы {group_id}: {str(e)}")

@app.get("/api/teachers/search", response_model=List[TeacherSearchResult])
async def search_teacher_endpoint(q: str = Query(..., min_length=2, description="Фамилия преподавателя")):
    query = q.strip()
    cache_key = f"teacher_search_{query.lower()}"
    cached = cache.get(cache_key)
    if cached:
        return cached
        
    try:
        results = await search_teachers(query)
        cache.set(cache_key, results, ttl=600)  # 10 mins
        return results
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Ошибка поиска преподавателя: {str(e)}")

@app.get("/api/schedule/teacher/{teacher_id}", response_model=ScheduleResponse)
async def get_teacher_schedule(teacher_id: str, refresh: bool = False):
    cache_key = f"schedule_teacher_{teacher_id}"
    if not refresh:
        cached = cache.get(cache_key)
        if cached:
            return cached
            
    try:
        sched = await parse_teacher_schedule(teacher_id)
        cache.set(cache_key, sched, ttl=1800)  # 30 mins
        return sched
    except Exception as e:
        raise HTTPException(status_code=502, detail=f"Ошибка получения расписания преподавателя {teacher_id}: {str(e)}")
