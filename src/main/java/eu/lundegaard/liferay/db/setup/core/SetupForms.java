/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Lundegaard a.s.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package eu.lundegaard.liferay.db.setup.core;

import com.liferay.dynamic.data.mapping.exception.NoSuchStructureException;
import com.liferay.dynamic.data.mapping.model.DDMFormInstance;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.service.DDMFormInstanceLocalServiceUtil;
import com.liferay.dynamic.data.mapping.service.DDMStructureLayoutLocalServiceUtil;
import com.liferay.dynamic.data.mapping.service.DDMStructureLocalServiceUtil;
import com.liferay.dynamic.data.mapping.storage.DDMFormValues;
import com.liferay.portal.kernel.exception.PortalException;
import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.service.ClassNameLocalServiceUtil;
import com.liferay.portal.kernel.service.ServiceContext;
import eu.lundegaard.liferay.db.setup.domain.Form;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static java.util.stream.Collectors.toMap;

/**
 * @author michal.volf@lundegaard.eu
 */
public final class SetupForms {

    private static final Log LOG = LogFactoryUtil.getLog(SetupForms.class);

    private SetupForms() {}

    public static void handleForms(List<Form> formList, long userId, long groupId) {
        for (Form form : formList) {
            LOG.info("Executing " + form.getSetupAction() + " on form " + getDefaultFormName(form));
            switch (form.getSetupAction()) {
                case "create":
                    createForm(userId, groupId, form);
                    break;
                case "update":
                    updateForm(userId, groupId, form);
                    break;
                case "delete":
                    deleteForm(groupId, form);
                    break;
                default:
                    throw new IllegalArgumentException("Illegal setup action " + form.getSetupAction());
            }
        }
    }

    public static void createForm(long userId, long groupId, Form form) {
        String defaultFormName = getDefaultFormName(form);
        long structureClassNameId = ClassNameLocalServiceUtil.getClassNameId(DDMFormInstance.class);

        try {
            DDMStructure retrievedStructure =
                    DDMStructureLocalServiceUtil.getStructure(groupId, structureClassNameId, form.getFormDbKey());
            LOG.warn("Form " + retrievedStructure.getNameCurrentValue() + " with key " + form.getFormDbKey()
                    + " already exists, skipping...");
        } catch (NoSuchStructureException e) {
            LOG.info("Creating form " + defaultFormName);
            createFormInstanceInDB(userId, groupId, form, structureClassNameId);
        } catch (PortalException e) {
            LOG.error("Error during setup of form " + defaultFormName, e);
        }
    }

    private static void createFormInstanceInDB(long userId, long groupId, Form form, long structureClassNameId) {
        try {
            ServiceContext serviceContext = new ServiceContext();
            Map<Locale, String> nameMap = namesListToMap(form.getFormName().getName());
            Map<Locale, String> descriptionMap = descriptionsListToMap(form.getFormDescription().getDescription());
            String structureKey = form.getFormDbKey();
            String structureLayoutKey = structureKey + "_LAYOUT";

            DDMStructure ddmStructure = DDMStructureLocalServiceUtil.addStructure(
                    userId,
                    groupId,
                    0,
                    structureClassNameId,
                    structureKey,
                    nameMap,
                    descriptionMap,
                    form.getFormData(),
                    "json",
                    serviceContext);

            DDMFormInstanceLocalServiceUtil.addFormInstance(
                    userId,
                    groupId,
                    ddmStructure.getStructureId(),
                    nameMap,
                    descriptionMap,
                    form.getFormSettings(),
                    serviceContext);

            DDMStructureLayoutLocalServiceUtil.addStructureLayout(
                    userId,
                    groupId,
                    0,
                    structureLayoutKey,
                    ddmStructure.getStructureVersion().getStructureVersionId(),
                    nameMap,
                    descriptionMap,
                    form.getFormLayout(),
                    serviceContext);

            LOG.info("Form created successfully");
        } catch (PortalException e) {
            LOG.error("Creating the form threw an error", e);
        }
    }

    private static void updateForm(long userId, long groupId, Form form) {
        String defaultFormName = getDefaultFormName(form);
        long structureClassNameId = ClassNameLocalServiceUtil.getClassNameId(DDMFormInstance.class);

        try {
            DDMStructure retrievedStructure =
                    DDMStructureLocalServiceUtil.getStructure(groupId, structureClassNameId, form.getFormDbKey());
            LOG.info("Form " + retrievedStructure.getNameCurrentValue() + " with key " + form.getFormDbKey()
                    + " found, updating...");
            updateFormInstanceInDB(userId, groupId, form, retrievedStructure.getStructureId());
        } catch (NoSuchStructureException e) {
            LOG.warn("Form " + defaultFormName + " not found, cannot be updated");
        } catch (PortalException e) {
            LOG.error("Error during update of form " + defaultFormName, e);
        }
    }

    private static void updateFormInstanceInDB(long userId, long groupId, Form form, long oldStructureId) {
        try {
            Map<Locale, String> nameMap = namesListToMap(form.getFormName().getName());
            Map<Locale, String> descriptionMap = descriptionsListToMap(form.getFormDescription().getDescription());
            ServiceContext serviceContext = new ServiceContext();

            DDMStructure ddmStructure = DDMStructureLocalServiceUtil.updateStructure(
                    userId,
                    oldStructureId,
                    0,
                    nameMap,
                    descriptionMap,
                    form.getFormData(),
                    serviceContext);

            String oldStructureLayoutKey = form.getFormDbKey() + "_LAYOUT";
            long oldStructureLayoutId = DDMStructureLayoutLocalServiceUtil
                    .getStructureLayout(groupId, 0, oldStructureLayoutKey).getStructureLayoutId();
            DDMStructureLayoutLocalServiceUtil.updateStructureLayout(
                    oldStructureLayoutId,
                    ddmStructure.getStructureVersion().getStructureVersionId(),
                    nameMap,
                    descriptionMap,
                    form.getFormLayout(),
                    serviceContext);

            long oldFormInstanceId = DDMFormInstanceLocalServiceUtil.getFormInstances(groupId).stream()
                    .filter(ddmFormInstance -> ddmFormInstance.getStructureId() == oldStructureId).findFirst()
                    .orElseThrow(IllegalStateException::new).getFormInstanceId();
            DDMFormInstanceLocalServiceUtil.updateFormInstance(
                    oldFormInstanceId,
                    ddmStructure.getStructureId(),
                    nameMap,
                    descriptionMap,
                    new DDMFormValues(ddmStructure.getDDMForm()),
                    serviceContext);

            LOG.info("Form updated successfully");
        } catch (PortalException e) {
            LOG.error("Updating the form threw an error", e);
        }
    }

    private static void deleteForm(long groupId, Form form) {
        String defaultFormName = getDefaultFormName(form);
        long structureClassNameId = ClassNameLocalServiceUtil.getClassNameId(DDMFormInstance.class);

        try {
            DDMStructure retrievedStructure =
                    DDMStructureLocalServiceUtil.getStructure(groupId, structureClassNameId, form.getFormDbKey());
            LOG.info("Form " + retrievedStructure.getNameCurrentValue() + " with key " + form.getFormDbKey()
                    + " found, deleting...");
            deleteFormInstanceInDB(groupId, form, retrievedStructure.getStructureId());
        } catch (NoSuchStructureException e) {
            LOG.warn("Form " + defaultFormName + " not found, cannot be deleted");
        } catch (PortalException e) {
            LOG.error("Error during delete of form " + defaultFormName, e);
        }
    }

    private static void deleteFormInstanceInDB(long groupId, Form form, long structureId) {
        try {
            long formInstanceId = DDMFormInstanceLocalServiceUtil.getFormInstances(groupId).stream()
                    .filter(ddmFormInstance -> ddmFormInstance.getStructureId() == structureId).findFirst()
                    .orElseThrow(IllegalStateException::new).getFormInstanceId();

            String structureLayoutKey = form.getFormDbKey() + "_LAYOUT";
            long oldStructureLayoutId = DDMStructureLayoutLocalServiceUtil
                    .getStructureLayout(groupId, 0, structureLayoutKey).getStructureLayoutId();

            DDMStructureLocalServiceUtil.deleteDDMStructure(structureId);
            DDMFormInstanceLocalServiceUtil.deleteDDMFormInstance(formInstanceId);
            DDMStructureLayoutLocalServiceUtil.deleteStructureLayout(oldStructureLayoutId);

            LOG.info("Form deleted successfully");
        } catch (PortalException e) {
            LOG.error("Updating the form threw an error", e);
        }
    }

    private static String getDefaultFormName(Form form) {
        return namesListToMap(form.getFormName().getName())
                .getOrDefault(Locale.forLanguageTag(fixLocaleString(form.getFormName().getDefaultLocale())),
                        "unknown name");
    }

    private static Map<Locale, String> namesListToMap(List<Form.FormName.Name> list) {
        return list.stream().collect(
                toMap(
                        name -> Locale.forLanguageTag(fixLocaleString(name.getLanguageId())),
                        name -> name.getValue()));
    }

    private static Map<Locale, String> descriptionsListToMap(List<Form.FormDescription.Description> list) {
        return list.stream().collect(
                toMap(
                        description -> Locale.forLanguageTag(fixLocaleString(description.getLanguageId())),
                        description -> description.getValue()));
    }

    private static String fixLocaleString(String localeString) {
        return localeString.replace('_', '-');
    }

}
