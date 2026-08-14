package com.java_web.backend.Teacher.Service;

import com.java_web.backend.Common.DTO.PromptTemplateDTO;
import com.java_web.backend.Common.Exception.ApiException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PromptTemplateService {
    private static final Set<String> SUPPORTED_MODULES = Set.of("objective", "syllabus", "material", "courseware");

    private static final List<PromptTemplateDTO> PROMPT_TEMPLATES = List.of(
            new PromptTemplateDTO(
                    "objective-general",
                    "objective",
                    "课程介绍模板",
                    "专业基础课通用模板",
                    "适合本科专业基础课程，突出课程定位、先修基础和学习价值。",
                    "请以高校本科课程建设文风，生成一门专业基础课的课程介绍与教学目标。课程介绍部分说明课程定位、适用专业、先修基础和学习价值；教学目标部分按知识目标、能力目标、素养目标分点描述，语言正式清晰，可直接用于教学文档。"
            ),
            new PromptTemplateDTO(
                    "objective-obe",
                    "objective",
                    "教学目标模板",
                    "OBE成果导向模板",
                    "突出知识、能力、素养与学习产出的对应关系。",
                    "请基于 OBE 成果导向教学理念生成课程介绍与教学目标。课程介绍需要说明课程如何支撑专业培养；教学目标围绕知识、能力、素养和可观察学习成果展开，适合本科课程申报或教学大纲使用。"
            ),
            new PromptTemplateDTO(
                    "objective-practice",
                    "objective",
                    "课程介绍与教学目标模板",
                    "实践项目课模板",
                    "适合实验、项目驱动或综合实践类课程。",
                    "请为一门强调实验与项目实践的课程生成课程介绍与教学目标。课程介绍突出应用场景、项目驱动特点和实践价值；教学目标体现动手能力、团队协作、问题分析与工程表达要求。"
            ),
            new PromptTemplateDTO(
                    "syllabus-16-week",
                    "syllabus",
                    "16周课程大纲模板",
                    "标准16周教学安排",
                    "按16周组织教学主题、重点难点和作业安排。",
                    "请按16周教学安排生成课程大纲，每周包含教学主题、核心知识点、课堂活动或案例、作业建议，并兼顾理论与实践比例，输出结构清晰、适合直接编辑的 Markdown 大纲。"
            ),
            new PromptTemplateDTO(
                    "syllabus-project",
                    "syllabus",
                    "16周课程大纲模板",
                    "项目制课程模板",
                    "突出阶段任务、里程碑与考核安排。",
                    "请生成一份项目制课程大纲，建议按16周或阶段任务展开，明确每阶段主题、实践任务、阶段成果、里程碑检查点和考核方式，适合软件类或工程类课程。"
            ),
            new PromptTemplateDTO(
                    "syllabus-theory-practice",
                    "syllabus",
                    "16周课程大纲模板",
                    "理论实验并行模板",
                    "适合同时包含理论授课和实验上机的课程。",
                    "请生成一份理论授课与实验实践并行的课程大纲，建议按16周安排，每周说明理论内容、实验或上机内容、课后任务和学时分配，强调知识递进关系。"
            ),
            new PromptTemplateDTO(
                    "material-chapter",
                    "material",
                    "章节讲义模板",
                    "章节讲解模板",
                    "包含学习目标、核心概念、示例解析和课后练习。",
                    "请围绕某一章节生成教学讲义，内容包含学习目标、核心概念解释、示例解析、课堂互动问题和课后练习，整体语言清晰、层次分明，适合本科课堂教学。"
            ),
            new PromptTemplateDTO(
                    "material-case",
                    "material",
                    "章节讲义模板",
                    "案例驱动模板",
                    "通过案例导入知识点，适合应用型课程。",
                    "请生成一份案例驱动型教学讲义，采用“场景导入-问题分析-知识讲解-案例总结-常见误区”的结构，强调知识与真实应用场景的联系。"
            ),
            new PromptTemplateDTO(
                    "material-review",
                    "material",
                    "章节讲义模板",
                    "复习串讲模板",
                    "适合考前复习或章节总结课。",
                    "请生成一份复习串讲型教学讲义，包含知识框架梳理、重点难点回顾、典型例题或案例、易错点提醒和自测题，适合作为阶段复习资料。"
            ),
            new PromptTemplateDTO(
                    "courseware-overview",
                    "courseware",
                    "课件提纲模板",
                    "标准课件提纲模板",
                    "适合根据课程大纲快速生成课堂展示用课件提纲。",
                    "请根据课程大纲生成一份适用于课堂展示的教学课件提纲，包含课程定位、教学目标、章节安排、重点难点、案例互动、总结与作业等部分，并为每一部分给出建议页标题和要点。"
            ),
            new PromptTemplateDTO(
                    "courseware-practice",
                    "courseware",
                    "课件提纲模板",
                    "实践导向模板",
                    "强调实验、案例和课堂活动设计，适合应用型课程。",
                    "请生成一份偏实践导向的教学课件提纲，突出实验任务、案例讨论、课堂互动和阶段成果展示，适合软件工程、数据分析或工程实践类课程。"
            ),
            new PromptTemplateDTO(
                    "courseware-review",
                    "courseware",
                    "课件提纲模板",
                    "复习串讲模板",
                    "适合阶段复习、章节总结或考前串讲场景。",
                    "请围绕课程大纲生成一份复习串讲型课件提纲，突出知识框架梳理、重点难点回顾、典型例题或案例和课后自测，适合阶段总结课使用。"
            )
    );

    public List<PromptTemplateDTO> getPromptTemplates(String module) {
        if (module == null || module.trim().isEmpty()) {
            return PROMPT_TEMPLATES;
        }

        String normalizedModule = module.trim().toLowerCase(Locale.ROOT);
        if (!SUPPORTED_MODULES.contains(normalizedModule)) {
            throw ApiException.badRequest("不支持的模板模块: " + module);
        }

        return PROMPT_TEMPLATES.stream()
                .filter(template -> template.getModule().equals(normalizedModule))
                .toList();
    }
}
