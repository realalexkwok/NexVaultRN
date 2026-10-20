import { NexVaultButton } from '@/components/NexVaultButton';
import { NexVaultCard } from '@/components/NexVaultCard';
import { NexVaultTextField } from '@/components/NexVaultTextField';
import { Screen } from '@/components/Screen';

// Send shell (original SendScreen.kt): disabled form. Transaction construction / native confirm
// / signDigest are T4.x (spec: ui-wallet §2.2). Honest "Not configured" (R5), no fake flow.
export default function SendScreen() {
  return (
    <Screen title="Send" showBack>
      <NexVaultCard className="gap-md">
        <NexVaultTextField
          label="Recipient"
          placeholder="Not configured"
          value=""
          editable={false}
          labelBackdrop="bg-card-surface"
        />
        <NexVaultTextField
          label="Amount"
          placeholder="Not configured"
          value=""
          editable={false}
          keyboardType="numeric"
          labelBackdrop="bg-card-surface"
        />
        <NexVaultButton label="Continue" disabled />
      </NexVaultCard>
    </Screen>
  );
}
