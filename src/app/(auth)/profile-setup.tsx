import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { Text, View } from 'react-native';

import { Button, Screen, TextField } from '@/components/ui';
import { useUpdateProfile } from '@/features/auth/hooks/use-auth';
import { profileSchema, type ProfileForm } from '@/features/auth/utils/profile-schema';
import { useAuthStore } from '@/stores/auth-store';

/** Lần đầu đăng nhập (`isNew`): đặt tên hiển thị, gợi ý sẵn tên Google do server điền (ADR-05). */
export default function ProfileSetup() {
  const suggested = useAuthStore((s) => s.user?.displayName ?? '');
  const update = useUpdateProfile();
  const { control, handleSubmit } = useForm<ProfileForm>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: suggested },
  });

  const submit = () =>
    void handleSubmit((form) =>
      update.mutate(form, { onSuccess: () => useAuthStore.getState().profileDone() }),
    )();

  return (
    <Screen className="gap-6 px-6 py-10">
      <View className="gap-2">
        <Text className="text-title font-semibold text-neutral-900 dark:text-neutral-100">
          Bạn muốn hiện tên gì?
        </Text>
        <Text className="text-body text-neutral-500 dark:text-neutral-400">
          Người khác thấy tên này khi xem tin bạn đăng. Đổi lại được trong hồ sơ.
        </Text>
      </View>
      <Controller
        control={control}
        name="displayName"
        render={({ field, fieldState }) => (
          <TextField
            label="Tên hiển thị"
            value={field.value}
            onChangeText={field.onChange}
            onBlur={field.onBlur}
            error={fieldState.error?.message}
            autoFocus
            maxLength={60}
            returnKeyType="done"
            onSubmitEditing={submit}
          />
        )}
      />
      {update.error ? (
        <Text accessibilityRole="alert" className="text-label text-danger-700 dark:text-danger-400">
          {update.error.message}
        </Text>
      ) : null}
      <Button title="Tiếp tục" loading={update.isPending} onPress={submit} />
    </Screen>
  );
}
