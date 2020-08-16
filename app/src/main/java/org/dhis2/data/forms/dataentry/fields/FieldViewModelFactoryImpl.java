package org.dhis2.data.forms.dataentry.fields;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.dhis2.data.forms.dataentry.fields.age.AgeViewModel;
import org.dhis2.data.forms.dataentry.fields.coordinate.CoordinateViewModel;
import org.dhis2.data.forms.dataentry.fields.datetime.DateTimeViewModel;
import org.dhis2.data.forms.dataentry.fields.edittext.EditTextViewModel;
import org.dhis2.data.forms.dataentry.fields.image.ImageViewModel;
import org.dhis2.data.forms.dataentry.fields.option_set.OptionSetViewModel;
import org.dhis2.data.forms.dataentry.fields.orgUnit.OrgUnitViewModel;
import org.dhis2.data.forms.dataentry.fields.picture.PictureViewModel;
import org.dhis2.data.forms.dataentry.fields.radiobutton.RadioButtonViewModel;
import org.dhis2.data.forms.dataentry.fields.scan.ScanTextViewModel;
import org.dhis2.data.forms.dataentry.fields.spinner.SpinnerViewModel;
import org.dhis2.data.forms.dataentry.fields.status.StatusHolder;
import org.dhis2.data.forms.dataentry.fields.status.StatusViewModel;
import org.dhis2.data.forms.dataentry.fields.statusbutton.StatusButtonViewModel;
import org.dhis2.data.forms.dataentry.fields.unsupported.UnsupportedViewModel;
import org.hisp.dhis.android.core.common.FeatureType;
import org.hisp.dhis.android.core.common.ObjectStyle;
import org.hisp.dhis.android.core.common.ValueType;
import org.hisp.dhis.android.core.common.ValueTypeDeviceRendering;
import org.hisp.dhis.android.core.common.ValueTypeRenderingType;
import org.hisp.dhis.android.core.program.ProgramStageSectionRenderingType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static android.text.TextUtils.isEmpty;
import static org.dhis2.utils.Preconditions.isNull;

public final class FieldViewModelFactoryImpl implements FieldViewModelFactory {

    @NonNull
    private final Map<ValueType, String> valueTypeHintMap;

    private final List<ValueTypeRenderingType> optionSetTextRenderings = Arrays.asList(
            ValueTypeRenderingType.HORIZONTAL_CHECKBOXES,
            ValueTypeRenderingType.VERTICAL_CHECKBOXES,
            ValueTypeRenderingType.HORIZONTAL_RADIOBUTTONS,
            ValueTypeRenderingType.VERTICAL_RADIOBUTTONS
    );

    public FieldViewModelFactoryImpl(Map<ValueType, String> valueTypeHintMap) {
        this.valueTypeHintMap = valueTypeHintMap;
    }

    @NonNull
    @Override
    @SuppressWarnings({
            "PMD.CyclomaticComplexity",
            "PMD.StdCyclomaticComplexity"
    })
    public FieldViewModel create(@NonNull String id, @NonNull String code, @NonNull String label, @NonNull ValueType type,
                                 @NonNull Boolean mandatory, @Nullable String optionSet, @Nullable String value,
                                 @Nullable String section, @Nullable Boolean allowFutureDates, @NonNull Boolean editable, @Nullable ProgramStageSectionRenderingType renderingType,
                                 @Nullable String description, @Nullable ValueTypeDeviceRendering fieldRendering, @Nullable Integer optionCount, ObjectStyle objectStyle, @Nullable String fieldMask) {
        isNull(type, "type must be supplied");

        if (!isEmpty(optionSet)) {
            if (renderingType == null || renderingType == ProgramStageSectionRenderingType.LISTING) {
                if (fieldRendering != null && (fieldRendering.type().equals(ValueTypeRenderingType.QR_CODE) || fieldRendering.type().equals(ValueTypeRenderingType.BAR_CODE))) {
                    return ScanTextViewModel.create(id, label, mandatory, value, section, editable, optionSet, description, objectStyle, fieldRendering);
                } else if (fieldRendering != null && type == ValueType.TEXT && optionSetTextRenderings.contains(fieldRendering.type())) {
                    return OptionSetViewModel.create(id, label, mandatory, optionSet, value, section, editable, description, objectStyle, fieldRendering);
                } else {
                    return SpinnerViewModel.create(id, label, valueTypeHintMap.get(type), mandatory, optionSet, value, section, editable, description, optionCount, objectStyle);
                }
            } else
                return ImageViewModel.create(id, label, optionSet, value, section, editable, mandatory, description, objectStyle); //transforms option set into image option selector
        }

        if(label.equalsIgnoreCase("biometrics")){
            return StatusButtonViewModel.create(id, label, mandatory, value, section, description, objectStyle);
        }else if (label.equalsIgnoreCase("Biometrics Verification")){
            return StatusViewModel.create(id, label, mandatory, value, section, description, objectStyle, StatusHolder.ValueStatus.NOT_DONE);
        }

        switch (type) {
            case AGE:
                return AgeViewModel.create(id, label, mandatory, value, section, editable, description, objectStyle);
            case TEXT:
            case EMAIL:
            case LETTER:
            case NUMBER:
            case INTEGER:
            case LONG_TEXT:
            case PERCENTAGE:
            case PHONE_NUMBER:
            case INTEGER_NEGATIVE:
            case INTEGER_POSITIVE:
            case INTEGER_ZERO_OR_POSITIVE:
            case UNIT_INTERVAL:
            case URL:
                if (fieldRendering != null && (fieldRendering.type().equals(ValueTypeRenderingType.QR_CODE) || fieldRendering.type().equals(ValueTypeRenderingType.BAR_CODE))) {
                    return ScanTextViewModel.create(id, label, mandatory, value, section, editable, optionSet, description, objectStyle, fieldRendering);
                } else {
                    return EditTextViewModel.create(id, code, label, mandatory, value, valueTypeHintMap.get(type), 1, type, section, editable, description, fieldRendering, objectStyle, fieldMask);
                }
            case IMAGE:
                return PictureViewModel.create(id, label, mandatory, value, section, editable, description, objectStyle);
            case TIME:
            case DATE:
            case DATETIME:
                return DateTimeViewModel.create(id, label, mandatory, type, value, section, allowFutureDates, editable, description, objectStyle);
            case COORDINATE:
                return CoordinateViewModel.create(id, label, mandatory, value, section, editable, description, objectStyle, FeatureType.POINT);
            case BOOLEAN:
            case TRUE_ONLY:
                return RadioButtonViewModel.fromRawValue(id, label, type, mandatory, value, section, editable, description, objectStyle,
                        fieldRendering != null ? fieldRendering.type() : ValueTypeRenderingType.DEFAULT);
            case ORGANISATION_UNIT:
                return OrgUnitViewModel.create(id, label, mandatory, value, section, editable, description, objectStyle);
            case FILE_RESOURCE:
            case TRACKER_ASSOCIATE:
            case USERNAME:
                return UnsupportedViewModel.create(id, label, mandatory, value, section, editable, description, objectStyle);
            default:
                return EditTextViewModel.create(id, code, label, mandatory, value, valueTypeHintMap.get(type), 1, type, section, editable, description, fieldRendering, objectStyle, fieldMask);
        }
    }
}
