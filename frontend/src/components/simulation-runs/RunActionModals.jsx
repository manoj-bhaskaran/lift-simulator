// @ts-check
import AlertModal from '../AlertModal';
import ConfirmModal from '../ConfirmModal';

function RunActionModals({
  runToDelete,
  bulkAction,
  selectedCount,
  actionError,
  actionErrorTitle,
  onCloseDelete,
  onConfirmDelete,
  onCloseBulkAction,
  onConfirmBulkAction,
  onCloseError,
}) {
  return (
    <>
      <ConfirmModal
        isOpen={runToDelete !== null}
        onClose={onCloseDelete}
        onConfirm={onConfirmDelete}
        title="Delete simulation run?"
        message={runToDelete
          ? `This will permanently delete run #${runToDelete.id}, including its run history `
            + 'and all stored artefacts (generated input, logs, and result files). '
            + 'This action cannot be undone.'
          : ''}
        confirmText="Delete run"
        cancelText="Keep run"
        confirmStyle="danger"
      />

      <ConfirmModal
        isOpen={bulkAction !== null}
        onClose={onCloseBulkAction}
        onConfirm={onConfirmBulkAction}
        title={bulkAction === 'cancel' ? 'Cancel selected runs?' : 'Delete selected runs?'}
        message={bulkAction === 'cancel'
          ? `This will request cancellation for ${selectedCount} selected active run${selectedCount === 1 ? '' : 's'}.`
          : `This will permanently delete ${selectedCount} selected completed run${selectedCount === 1 ? '' : 's'}, including history and stored artefacts. This action cannot be undone.`}
        confirmText={bulkAction === 'cancel' ? 'Cancel runs' : 'Delete runs'}
        cancelText="Keep runs"
        confirmStyle={bulkAction === 'delete' ? 'danger' : 'primary'}
      />

      <AlertModal
        isOpen={actionError !== null}
        onClose={onCloseError}
        title={actionErrorTitle}
        message={actionError || ''}
        type="error"
      />
    </>
  );
}

export default RunActionModals;
