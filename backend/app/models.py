from typing import List, Optional
from pydantic import BaseModel, Field

class GroupInfo(BaseModel):
    id: str = Field(..., description="Идентификатор группы в системе СПбГМТУ (например, 7738)")
    name: str = Field(..., description="Название группы (например, 10274)")
    url: str = Field(..., description="Относительный URL страницы расписания")

class CourseInfo(BaseModel):
    course: str = Field(..., description="Название курса (например, 1 курс)")
    groups: List[GroupInfo] = Field(default_factory=list, description="Список групп на курсе")

class FacultyInfo(BaseModel):
    faculty: str = Field(..., description="Название факультета или института")
    courses: List[CourseInfo] = Field(default_factory=list, description="Список курсов")

class TeacherSearchResult(BaseModel):
    id: str = Field(..., description="Идентификатор преподавателя (например, 101505)")
    name: str = Field(..., description="ФИО преподавателя (например, Альбаев Данил Айдарович)")
    url: str = Field(..., description="Относительный URL страницы расписания")

class Lesson(BaseModel):
    time: str = Field(..., description="Время проведения пары (например, 08:30 - 10:00)")
    week_type: str = Field(..., description="Тип недели: верхняя, нижняя или обе")
    dates: str = Field(..., description="Даты действия занятия")
    classroom: str = Field(..., description="Аудитория и корпус (например, 536(2) Корпус У)")
    group: str = Field(default="", description="Номер группы (заполняется в расписании преподавателя)")
    subject: str = Field(..., description="Название дисциплины")
    lesson_type: Optional[str] = Field(None, description="Вид занятия: Лекция, Практическое занятие, Лабораторное занятие и т.д.")
    subgroup: Optional[str] = Field(None, description="Подгруппа: 1-я п/г, 2-я п/г или None")
    teacher_name: str = Field(default="", description="ФИО преподавателя (в расписании групп)")
    teacher_id: str = Field(default="", description="ID преподавателя (если есть ссылка)")

class DaySchedule(BaseModel):
    day_name: str = Field(..., description="День недели (Понедельник, Вторник и т.д.)")
    lessons: List[Lesson] = Field(default_factory=list, description="Список занятий в этот день")

class ScheduleResponse(BaseModel):
    id: str = Field(..., description="ID группы или преподавателя")
    title: str = Field(..., description="Заголовок (номер группы или ФИО преподавателя)")
    is_teacher: bool = Field(False, description="Признак: True если расписание преподавателя, False если группы")
    days: List[DaySchedule] = Field(default_factory=list, description="Список дней недели с занятиями")
    cached_at: Optional[str] = Field(None, description="ISO время кэширования")
