from sys import exit
import xgboost as xgb
import numpy as np
import pandas


model_name = 'brits'

loaded = np.load('../resources/{}_data_epoch_100_iw_0.3.npy'.format(model_name))

print("Loaded data shape: ", loaded.shape)  # (3997, 48, 35)
# TODO somehow also get and places record ids to the first dimension, and save the file in text format


impute = loaded.reshape(-1, 48 * 35)
label = np.load('../resources/{}_label_epoch_100_iw_0.3.npy'.format(model_name))

data = np.nan_to_num(impute)

n_train = 3000

print(impute.shape)
print(label.shape)

#print("impute is : ")
#print(impute)

#print("data is : ")
#row_labels = range(3997)
#column_labels = range(1680)
#print pandas.DataFrame(data, columns=column_labels, index=row_labels)

#'''
#exit(0)


from sklearn.ensemble import RandomForestClassifier
from sklearn.metrics import roc_auc_score, confusion_matrix, classification_report, average_precision_score

auc = []

for i in range(10):
    # n_estimators - the number of trees in the forest
    model = RandomForestClassifier(n_estimators=100).fit(data[:n_train], label[:n_train])
    pred = model.predict_proba(data[n_train:])

    rfc_pred = np.argmax(pred, axis=1)

    #print(label[n_train:].reshape(-1,).tolist())
    #print(rfc_pred.tolist())

    labels_1d = label[n_train:].reshape(-1,)

    cur_auc = roc_auc_score(labels_1d, pred[:, 1].reshape(-1, ))
    cur_ap = average_precision_score(labels_1d, pred[:, 1].reshape(-1, ))

    tn, fp, fn, tp = confusion_matrix(labels_1d, rfc_pred).ravel()

    print("=== Confusion Matrix ===")
    print(confusion_matrix(labels_1d, rfc_pred))
    print("tp = " + str(tp), "fn = " + str(fn), "fp = " + str(fp), "tn = " + str(tn))
    #print('\n')
    #print("=== Classification Report ===")
    #print(classification_report(labels_1d, rfc_pred))
    #print('\n')

    print("Run " + str(i+1) + ": ", "AUROC: " + str(cur_auc), ", AUPRC: " + str(cur_ap))
    auc.append(cur_auc)

print("Mean AUC: ", np.mean(auc))
#'''
exit(0)