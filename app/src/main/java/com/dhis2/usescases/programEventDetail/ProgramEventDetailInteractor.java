package com.dhis2.usescases.programEventDetail;

import android.annotation.SuppressLint;
import android.support.annotation.IntDef;

import com.dhis2.Bindings.Bindings;
import com.dhis2.data.metadata.MetadataRepository;
import com.dhis2.utils.DateUtils;
import com.dhis2.utils.OrgUnitUtils;
import com.dhis2.utils.Period;

import org.hisp.dhis.android.core.category.CategoryComboModel;
import org.hisp.dhis.android.core.category.CategoryOptionComboModel;
import org.hisp.dhis.android.core.event.EventModel;
import org.hisp.dhis.android.core.program.ProgramModel;
import org.hisp.dhis.android.core.trackedentity.TrackedEntityDataValueModel;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * Created by Cristian on 13/02/2018.
 */

public class ProgramEventDetailInteractor implements ProgramEventDetailContract.Interactor {

    private final MetadataRepository metadataRepository;
    private final ProgramEventDetailRepository programEventDetailRepository;
    private ProgramEventDetailContract.View view;
    private String programId;
    private CompositeDisposable compositeDisposable;
    private CategoryOptionComboModel categoryOptionComboModel;

    private Date fromDate;
    private Date toDate;

    private List<Date> dates;
    private Period period;

    private @LastSearchType
    int lastSearchType;
    private CategoryComboModel mCatCombo;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({LastSearchType.DATES, LastSearchType.DATE_RANGES})
    public @interface LastSearchType {
        int DATES = 1;
        int DATE_RANGES = 32;
    }

    ProgramEventDetailInteractor(ProgramEventDetailRepository programEventDetailRepository, MetadataRepository metadataRepository) {
        this.metadataRepository = metadataRepository;
        this.programEventDetailRepository = programEventDetailRepository;
        Bindings.setMetadataRepository(metadataRepository);
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void init(ProgramEventDetailContract.View view, String programId, Period period) {
        this.view = view;
        this.programId = programId;
        getProgram();
        getOrgUnits(null);
        switch (period) {
            case DAILY:
                Date[] datesToQuery = DateUtils.getInstance().getDateFromDateAndPeriod(view.getChosenDateDay(), period);
                getEvents(programId, datesToQuery[0], datesToQuery[1]);
                break;
            case WEEKLY:
                getProgramEventsWithDates(programId, view.getChosenDateWeek(), period);
                break;
            case MONTHLY:
                getProgramEventsWithDates(programId, view.getChosenDateMonth(), period);
                break;
            case YEARLY:
                getProgramEventsWithDates(programId, view.getChosenDateYear(), period);
                break;

            default:
                getEvents(programId, DateUtils.getInstance().getToday(), DateUtils.getInstance().getToday());
                break;
        }

    }

    @SuppressLint("CheckResult")
    @Override
    public void getEvents(String programId, Date fromDate, Date toDate) {
        this.fromDate = fromDate;
        this.toDate = toDate;
        lastSearchType = LastSearchType.DATES;
        Observable.just(programEventDetailRepository.filteredProgramEvents(programId,
                DateUtils.getInstance().formatDate(fromDate),
                DateUtils.getInstance().formatDate(toDate),
                categoryOptionComboModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        view::setData,
                        Timber::e));
    }

    @Override
    public void getOrgUnits(Date date) {
        compositeDisposable.add(programEventDetailRepository.orgUnits()
               /* .debounce(500, TimeUnit.MILLISECONDS)
                .flatMapIterable(organisationUnitModels -> organisationUnitModels)
                .filter(orgUnit -> orgUnit.openingDate() != null && orgUnit.closedDate() != null)
                .filter(orgUnit -> orgUnit.openingDate().compareTo(date) <= 0 && orgUnit.closedDate().compareTo(date) >= 0)
                .toList()*/
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        orgUnits -> view.addTree(OrgUnitUtils.renderTree(view.getContext(), orgUnits)),
                        throwable -> view.renderError(throwable.getMessage())
                ));
    }

    @Override
    public void getProgramEventsWithDates(String programId, List<Date> dates, Period period) {
        this.dates = dates;
        this.period = period;
        lastSearchType = LastSearchType.DATE_RANGES;
        compositeDisposable.add(programEventDetailRepository.filteredProgramEvents(programId, dates, period, categoryOptionComboModel)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        view::setData,
                        throwable -> view.renderError(throwable.getMessage())));
    }

    @Override
    public void updateFilters(CategoryOptionComboModel categoryOptionComboModel) {
        this.categoryOptionComboModel = categoryOptionComboModel;
        switch (lastSearchType) {
            case LastSearchType.DATES:
                getEvents(programId, this.fromDate, this.toDate);
                break;
            case LastSearchType.DATE_RANGES:
                getProgramEventsWithDates(programId, this.dates, this.period);
                break;
            default:
                getEvents(programId, DateUtils.getInstance().getToday(), DateUtils.getInstance().getToday());
                break;
        }
    }

    @Override
    public Observable<List<TrackedEntityDataValueModel>> getEventDataValue(EventModel event) {
        return programEventDetailRepository.eventDataValues(event);
    }


    private void getProgram() {
        compositeDisposable.add(metadataRepository.getProgramWithId(programId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        (programModel) -> {
                            view.setProgram(programModel);
                            getCatCombo(programModel);
                        },
                        Timber::d)
        );
    }

    private void getCatCombo(ProgramModel programModel) {
        compositeDisposable.add(metadataRepository.getCategoryComboWithId(programModel.categoryCombo())
                .filter(categoryComboModel -> categoryComboModel != null && !categoryComboModel.uid().equals(CategoryComboModel.DEFAULT_UID))
                .flatMap(catCombo -> {
                    mCatCombo = catCombo;
                    return programEventDetailRepository.catCombo(programModel.categoryCombo());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(catComboOptions -> view.setCatComboOptions(mCatCombo, catComboOptions), Timber::d)
        );
    }

    @Override
    public void onDettach() {
        compositeDisposable.clear();
    }
}
